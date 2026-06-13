package com.bazaaralert.client.parser;

import com.bazaaralert.client.model.BazaarOrder;
import com.bazaaralert.client.model.OrderType;
import com.bazaaralert.client.util.TextUtils;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatOrderParser {
	// More tolerant regexes to match Minecraft punctuation/formatting variations.
	// Examples seen in chat:
	//  - "... for 9911.9 coins." (dot)
	//  - "... for 10037.0 coins." (decimal .0)
	//  - sometimes "coins!" / other trailing punctuation
	private static final Pattern BUY_ORDER = Pattern.compile(
		"\\[Bazaar] Buy Order Setup!\\s+([\\d,]+)x\\s+(.*)\\s+for\\s+([\\d,.]+)\\s+coins[.!]"
	);
	private static final Pattern SELL_OFFER = Pattern.compile(
		"\\[Bazaar] Sell Offer Setup!\\s+([\\d,]+)x\\s+(.*)\\s+for\\s+([\\d,.]+)\\s+coins[.!]"
	);

	private static final Pattern CANCELLED = Pattern.compile(
		"\\[Bazaar] Cancelled!?\\s+Refunded.*?\\b(?:Buy Order|Sell Offer)\\b"
	);

	// Cancel message variants we can actually extract the product from in this mod.
	private static final Pattern CANCELLED_BUY = Pattern.compile(
		"\\[Bazaar] Cancelled!\\s+Refunded\\s+([\\d,]+)\\s+coins\\s+from\\s+cancelling\\s+Buy Order!"
	);
	private static final Pattern CANCELLED_SELL = Pattern.compile(
		"\\[Bazaar] Cancelled!\\s+Refunded\\s+([\\d,]+)\\s+coins\\s+from\\s+cancelling\\s+Sell Offer!"
	);

	// Claim/filled variants
	private static final Pattern FILLED_SELL = Pattern.compile(
		"\\[Bazaar] Your Sell Offer for\\s+([\\d,]+)x\\s+(.*?)\\s+was filled!"
	);
	private static final Pattern FILLED_BUY = Pattern.compile(
		"\\[Bazaar] Your Buy Order for\\s+([\\d,]+)x\\s+(.*?)\\s+was filled!"
	);

	// Coflnet ghost-order cleanup (selling claim)
	// Example: "[Bazaar] Claimed 123456.7 coins from selling COAL at 2.0 each!"
	private static final Pattern CLAIMED_FROM_SELLING = Pattern.compile(
		"\\[Bazaar] Claimed\\s+([\\d,.]+)\\s+coins\\s+from\\s+selling\\s+(.*?)\\s+at\\s+([\\d,.]+)\\s+each!"
	);


	private static final Pattern OFFER_SETUP_SELL = Pattern.compile(
		"\\[Bazaar] Sell Offer Setup!\\s+([\\d,]+)x\\s+(.*?)\\s+for\\s+([\\d,.]+)\\s+"
	);

	private ChatOrderParser() {
	}


	public static Optional<BazaarOrder> parseSetupMessage(String message) {
		Matcher buyMatcher = BUY_ORDER.matcher(message);
		if (buyMatcher.matches()) {
			return buildOrder(buyMatcher, OrderType.BUY);
		}

		Matcher sellMatcher = SELL_OFFER.matcher(message);
		if (sellMatcher.matches()) {
			return buildOrder(sellMatcher, OrderType.SELL);
		}

		return Optional.empty();
	}


	public static Optional<String> parseCancelledProductId(String message) {
		// Prefer extracting item name from the cancel message when available.
		// Fallback to empty if we can't safely determine which product was cancelled.

		Matcher buy = CANCELLED.matcher(message);
		if (buy.matches()) {
			// Regex has a (legacy) group for itemName.
			if (buy.groupCount() >= 2) {
				String itemName = buy.group(2).trim();
				return Optional.ofNullable(itemName.isEmpty() ? null : TextUtils.displayNameToProductId(itemName));
			}
		}

		// If we only match the simplified cancel variants (no item name captured),
		// we cannot remove reliably.
		Matcher buySimple = CANCELLED_BUY.matcher(message);
		if (buySimple.matches()) {
			return Optional.empty();
		}

		Matcher sellSimple = CANCELLED_SELL.matcher(message);
		if (sellSimple.matches()) {
			return Optional.empty();
		}

		return Optional.empty();
	}

	public static Optional<OrderType> parseFilledOrderType(String message) {
		Matcher sell = FILLED_SELL.matcher(message);
		if (sell.matches()) {
			return Optional.of(OrderType.SELL);
		}
		Matcher buy = FILLED_BUY.matcher(message);
		if (buy.matches()) {
			return Optional.of(OrderType.BUY);
		}
		return Optional.empty();
	}

	public static Optional<String> parseClaimedProductIdFromSelling(String message) {
		Matcher claimed = CLAIMED_FROM_SELLING.matcher(message);
		if (!claimed.matches()) {
			return Optional.empty();
		}

		String itemName = claimed.group(2).trim();
		return Optional.of(TextUtils.displayNameToProductId(itemName));
	}

	public static Optional<String> parseFilledProductId(String message) {

		Matcher sell = FILLED_SELL.matcher(message);
		if (sell.matches()) {
			String itemName = sell.group(2).trim();
			return Optional.of(TextUtils.displayNameToProductId(itemName));
		}

		Matcher buy = FILLED_BUY.matcher(message);
		if (buy.matches()) {
			String itemName = buy.group(2).trim();
			return Optional.of(TextUtils.displayNameToProductId(itemName));
		}

		return Optional.empty();
	}

	private static Optional<BazaarOrder> buildOrder(Matcher matcher, OrderType type) {

		long amount = TextUtils.parseLong(matcher.group(1));
		String itemName = matcher.group(2).trim();
		double totalCoins = TextUtils.parseDouble(matcher.group(3));
		double pricePerUnit = totalCoins / amount;
		String productId = TextUtils.displayNameToProductId(itemName);
		return Optional.of(new BazaarOrder(productId, itemName, type, pricePerUnit, amount));
	}
}
