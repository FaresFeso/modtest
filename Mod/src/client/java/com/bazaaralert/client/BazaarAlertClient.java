package com.bazaaralert.client;

import com.bazaaralert.client.alert.AlertManager;
import com.bazaaralert.client.api.CoflnetBazaarApi;
import com.bazaaralert.client.checker.OutbidChecker;
import com.bazaaralert.client.config.BazaarAlertConfig;
import com.bazaaralert.client.model.BazaarOrder;
import com.bazaaralert.client.model.OrderType;
import com.bazaaralert.client.parser.ChatOrderParser;
import com.bazaaralert.client.parser.OrderInventoryParser;
import com.bazaaralert.client.tracker.OrderTracker;
import com.bazaaralert.client.util.HypixelSkyblockUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class BazaarAlertClient implements ClientModInitializer {
	private static final Logger LOGGER = LoggerFactory.getLogger(BazaarAlert.MOD_ID);

	private final BazaarAlertConfig config = new BazaarAlertConfig();
	private final OrderTracker orderTracker = new OrderTracker();
	private final CoflnetBazaarApi bazaarApi = new CoflnetBazaarApi();
	private final AlertManager alertManager = new AlertManager(config);
	private final OutbidChecker outbidChecker = new OutbidChecker(orderTracker, bazaarApi, alertManager);


	private int tickCounter;
	private long lastPollMillis;

	@Override
	public void onInitializeClient() {
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
				return;
			}

			String title = containerScreen.getTitle().getString();
			if (!OrderInventoryParser.isOrdersInventory(title)) {
				return;
			}

			var parsedOrders = OrderInventoryParser.parse(containerScreen);
			// Prevent wiping tracked orders during intermediate/empty inventory init states.
			if (parsedOrders.isEmpty()) {
				return;
			}

			orderTracker.replaceAll(parsedOrders);
			alertManager.clearAlertState();
			LOGGER.info("Synced {} bazaar orders from inventory", orderTracker.size());
		});

		ClientReceiveMessageEvents.GAME.register((message, overlay) -> handleChatMessage(message.getString()));

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(
				ClientCommandManager.literal("bazaaralert")
					.executes(context -> {
						sendStatusMessage();
						return 1;
					})
			);
			dispatcher.register(
				ClientCommandManager.literal("ba")
					.executes(context -> {
						sendStatusMessage();
						return 1;
					})
			);
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null || client.level == null) {
				return;
			}

			if (config.onlyOnHypixel && (!HypixelSkyblockUtils.isOnHypixel() || !HypixelSkyblockUtils.isLikelyInSkyblock())) {
				return;
			}

			tickCounter++;
			if (tickCounter % 20 != 0) {
				return;
			}

			long pollIntervalMillis = config.pollIntervalSeconds * 1000L;
			if (System.currentTimeMillis() - lastPollMillis < pollIntervalMillis) {
				return;
			}

			if (orderTracker.isEmpty()) {
				return;
			}

			lastPollMillis = System.currentTimeMillis();
			bazaarApi.refreshAsync().thenAccept(success -> {
				if (success) {
					Minecraft.getInstance().execute(outbidChecker::checkOrders);
				}
			});
		});


		LOGGER.info("Bazaar Alert loaded. Open Manage Orders in the Bazaar to sync your orders.");
	}

	private void handleChatMessage(String message) {
		Optional<BazaarOrder> setupOrder = ChatOrderParser.parseSetupMessage(message);
		if (setupOrder.isPresent()) {
			orderTracker.upsert(setupOrder.get());
			alertManager.markCompetitive(setupOrder.get());
			return;
		}

		// When an offer is filled, remove it from the tracker.
		ChatOrderParser.parseFilledProductId(message).ifPresent(productId -> {
			ChatOrderParser.parseFilledOrderType(message).ifPresent(type -> {
				orderTracker.remove(productId + ":" + type.name());
				alertManager.clearAlertState();
			});
		});

		// Coflnet ghost-order cleanup: claimed coins from selling removes the SELL offer.
		ChatOrderParser.parseClaimedProductIdFromSelling(message).ifPresent(productId -> {
			orderTracker.remove(productId + ":" + OrderType.SELL.name());
			alertManager.clearAlertState();
		});



		// Cancel messages: remove by productId when we can reliably extract the item.
		ChatOrderParser.parseCancelledProductId(message).ifPresent(productId -> {
			orderTracker.remove(productId + ":" + OrderType.BUY.name());
			orderTracker.remove(productId + ":" + OrderType.SELL.name());
			alertManager.clearAlertState();
		});
	}


	public void sendStatusMessage() {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}

		if (orderTracker.isEmpty()) {
			client.player.displayClientMessage(
				Component.literal("§e[Bazaar Alert] §7No orders tracked. Open §fManage Orders §7in the Bazaar to sync."),
				false
			);
			return;
		}

		client.player.displayClientMessage(
			Component.literal("§e[Bazaar Alert] §7Tracking §f" + orderTracker.size() + " §7orders:"),
			false
		);

		for (BazaarOrder order : orderTracker.getOrders()) {
			client.player.displayClientMessage(
				Component.literal(" §8- §f" + order.type() + " §7" + order.displayName()
					+ " §6" + String.format("%.1f", order.pricePerUnit()) + " coins"),
				false
			);
		}
	}
}
