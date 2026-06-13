package com.bazaaralert.client.alert;

import com.bazaaralert.client.config.BazaarAlertConfig;
import com.bazaaralert.client.model.BazaarOrder;
import com.bazaaralert.client.model.OrderType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class AlertManager {
	private final BazaarAlertConfig config;
	// Send repeated alerts while the condition remains true.
	// The caller (OutbidChecker) decides when to alert; we intentionally do not debounce here.
	private final Set<String> alertedKeys = new HashSet<>();

	public AlertManager(BazaarAlertConfig config) {
		this.config = config;
	}

	public void clearAlertState() {
		alertedKeys.clear();
	}

	public void markCompetitive(BazaarOrder order) {
		// No-op: repeated alerts are allowed.
	}

	public void notifyOutbid(BazaarOrder order, double competitorPrice) {

		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}

		String message = buildMessage(order, competitorPrice);
		if (config.chatAlerts) {
			client.player.displayClientMessage(Component.literal(message), false);
		}

		if (config.soundAlerts) {
			// Compatibility: LocalPlayer#playNotifySound signature/method name varies across MC versions.
			// Fallback to playing a sound via the world's sound manager when available.
			try {
				if (client.level != null) {
					client.level.playSound(
							null,
							client.player.getX(),
							client.player.getY(),
							client.player.getZ(),
							SoundEvents.NOTE_BLOCK_PLING.value(),
							SoundSource.MASTER,
							1.0F,
							0.5F
						);
				}
			} catch (Throwable ignored) {
				// no-op
			}
		}

	}

	private static String buildMessage(BazaarOrder order, double competitorPrice) {
		String action = order.type() == OrderType.BUY ? "overbid" : "undercut";
		return String.format(
			Locale.US,
			"§c[Bazaar Alert] §eYour %s §f%s §ewas %s! §7(You: §6%.1f§7, Top: §6%.1f§7)",
			order.type() == OrderType.BUY ? "buy order" : "sell offer",
			order.displayName(),
			action,
			order.pricePerUnit(),
			competitorPrice
		);
	}
}
