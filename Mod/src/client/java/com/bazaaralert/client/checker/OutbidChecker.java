package com.bazaaralert.client.checker;

import com.bazaaralert.client.alert.AlertManager;
import com.bazaaralert.client.api.BazaarProductData;
import com.bazaaralert.client.api.CoflnetBazaarApi;

import com.bazaaralert.client.model.BazaarOrder;
import com.bazaaralert.client.model.OrderType;
import com.bazaaralert.client.tracker.OrderTracker;

public final class OutbidChecker {
	private final OrderTracker orderTracker;
	private final CoflnetBazaarApi bazaarApi;

	private final AlertManager alertManager;

	private static double round1(double v) {
		return Math.round(v * 10.0d) / 10.0d;
	}


	public OutbidChecker(OrderTracker orderTracker, CoflnetBazaarApi bazaarApi, AlertManager alertManager) {

		this.orderTracker = orderTracker;
		this.bazaarApi = bazaarApi;
		this.alertManager = alertManager;
	}

	public void checkOrders() {
		if (orderTracker.isEmpty()) {
			com.bazaaralert.client.DebugUtil.sendChatOrLog(org.slf4j.LoggerFactory.getLogger(com.bazaaralert.client.BazaarAlert.MOD_ID), "[DBG] checkOrders: orderTracker empty");
			return;
		}
		if (!bazaarApi.hasData()) {
			com.bazaaralert.client.DebugUtil.sendChatOrLog(org.slf4j.LoggerFactory.getLogger(com.bazaaralert.client.BazaarAlert.MOD_ID), "[DBG] checkOrders: bazaarApi has no data yet");
			return;
		}

		for (BazaarOrder order : orderTracker.getOrders()) {
			bazaarApi.getProduct(order.productId()).ifPresent(product -> {
				com.bazaaralert.client.DebugUtil.sendChatOrLog(
						org.slf4j.LoggerFactory.getLogger(com.bazaaralert.client.BazaarAlert.MOD_ID),
						"[DBG] order=" + order.type() + " " + order.displayName()
							+ " my=" + order.pricePerUnit()
							+ " topBuy=" + product.topBuyPrice()
							+ " topSell=" + product.topSellPrice()
							+ " (BUY compares to topBuy, SELL compares to topSell)"
				);
				evaluate(order, product);
			});
		}

	}


	private void evaluate(BazaarOrder order, BazaarProductData product) {
		// Compare using 1-decimal-rounded values to match bazaar UI rounding.
		// (Coflnet prices are typically shown with 1 decimal precision.)
		final double tolerance = 0.015d; // 1.5% safety margin (kept)
		double myPrice = round1(order.pricePerUnit());



		if (order.type() == OrderType.BUY) {
			double top = product.topBuyPrice();
			if (Double.isNaN(top)) {
				return;
			}

			long topBidPrice = Math.round(top);
			// If someone else has a higher BUY bid, you are outbid.
			// Apply tolerance to avoid false positives caused by tax/fee rounding differences.
			// Condition: topBid > myPrice * (1 + tolerance)
			long threshold = Math.round(myPrice * (1.0d + tolerance));
			if (topBidPrice > threshold) {

				alertManager.notifyOutbid(order, topBidPrice);
			} else {
				alertManager.markCompetitive(order);
			}
			return;
		}

		double top = product.topSellPrice();
		if (Double.isNaN(top)) {
			return;
		}

		long topAskPrice = Math.round(top);
		// If someone else has a lower SELL ask, you are undercut.
		// Apply tolerance to avoid false positives caused by tax/fee rounding differences.
		// Condition: topAsk < myPrice * (1 - tolerance)
		long sellThreshold = Math.round(myPrice * (1.0d - tolerance));
		if (topAskPrice < sellThreshold) {

			alertManager.notifyOutbid(order, topAskPrice);
		} else {
			alertManager.markCompetitive(order);
		}
	}

}
