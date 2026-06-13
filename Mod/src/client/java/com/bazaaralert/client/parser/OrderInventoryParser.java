package com.bazaaralert.client.parser;

import com.bazaaralert.client.model.BazaarOrder;
import com.bazaaralert.client.model.OrderType;

import com.bazaaralert.client.util.TextUtils;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OrderInventoryParser {
	private static final Pattern ORDER_NAME = Pattern.compile("(?i)(BUY|SELL)\\s+(.*)");
	private static final Pattern PRICE_LINE = Pattern.compile("Price per unit: ([\\d,.]+) coins");
	private static final Pattern AMOUNT_LINE = Pattern.compile("Amount: ([\\d,]+)x");

	private OrderInventoryParser() {
	}

	public static boolean isOrdersInventory(String title) {
		return "Your Bazaar Orders".equals(title) || "Co-op Bazaar Orders".equals(title);
	}

	public static List<BazaarOrder> parse(AbstractContainerScreen<?> screen) {
		List<BazaarOrder> orders = new ArrayList<>();

		for (Slot slot : screen.getMenu().slots) {
			ItemStack stack = slot.getItem();
			if (stack.isEmpty()) {
				continue;
			}

			parseStack(stack).ifPresent(orders::add);
		}

		return orders;
	}

	private static java.util.Optional<BazaarOrder> parseStack(ItemStack stack) {
		String displayName = TextUtils.stripColor(stack.getHoverName().getString());
		Matcher nameMatcher = ORDER_NAME.matcher(displayName);
		if (!nameMatcher.matches()) {
			return java.util.Optional.empty();
		}

		OrderType type = "BUY".equalsIgnoreCase(nameMatcher.group(1)) ? OrderType.BUY : OrderType.SELL;
		String itemName = nameMatcher.group(2).trim();
		String productId = TextUtils.displayNameToProductId(itemName);


		double price = 0.0D;
		long amount = 0L;
		ItemLore lore = stack.get(DataComponents.LORE);
		if (lore != null) {
			for (Component line : lore.lines()) {
				String text = TextUtils.stripColor(line.getString());
				Matcher priceMatcher = PRICE_LINE.matcher(text);
				if (priceMatcher.matches()) {
					price = TextUtils.parseDouble(priceMatcher.group(1));
					continue;
				}

				Matcher amountMatcher = AMOUNT_LINE.matcher(text);
				if (amountMatcher.matches()) {
					amount = TextUtils.parseLong(amountMatcher.group(1));
				}
			}
		}

		if (price <= 0.0D) {
			return java.util.Optional.empty();
		}

		return java.util.Optional.of(new BazaarOrder(productId, itemName, type, price, amount));
	}
}
