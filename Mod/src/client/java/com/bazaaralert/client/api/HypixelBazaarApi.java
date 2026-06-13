package com.bazaaralert.client.api;

import com.bazaaralert.client.BazaarAlert;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class HypixelBazaarApi {
	private static final Logger LOGGER = LoggerFactory.getLogger(BazaarAlert.MOD_ID);
	private static final URI BAZAAR_URI = URI.create("https://api.hypixel.net/v2/skyblock/bazaar");
	private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(10))
		.build();

	private final Map<String, BazaarProductData> products = new HashMap<>();
	private volatile boolean fetchInProgress;
	private volatile long lastFetchMillis;

	public CompletableFuture<Boolean> refreshAsync() {
		if (fetchInProgress) {
			return CompletableFuture.completedFuture(false);
		}

		fetchInProgress = true;
		HttpRequest request = HttpRequest.newBuilder(BAZAAR_URI)
			.timeout(Duration.ofSeconds(15))
			.header("User-Agent", "BazaarAlert/1.0")
			.GET()
			.build();

		return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenApply(response -> {
				if (response.statusCode() != 200) {
					LOGGER.warn("Hypixel bazaar API returned status {}", response.statusCode());
					return false;
				}

				try {
					JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
					if (!root.get("success").getAsBoolean()) {
						LOGGER.warn("Hypixel bazaar API reported failure");
						return false;
					}

					parseProducts(root.getAsJsonObject("products"));
					lastFetchMillis = System.currentTimeMillis();
					return true;
				} catch (RuntimeException exception) {
					LOGGER.warn("Failed to parse Hypixel bazaar API response", exception);
					return false;
				}
			})
			.whenComplete((ignored, throwable) -> fetchInProgress = false);
	}

	private void parseProducts(JsonObject productsObject) {
		products.clear();
		for (Map.Entry<String, JsonElement> entry : productsObject.entrySet()) {
			String productId = entry.getKey();
			JsonObject product = entry.getValue().getAsJsonObject();
			// Hypixel bazaar summaries are ordered by best price for each side:
			// - buy_summary: bids (highest buy first)
			// - sell_summary: asks (lowest sell first)
			// IMPORTANT: Based on your screenshots/debug logs, our variable naming in
			// BazaarProductData is inverted versus what the in-game BUY/SELL menus display.
			// To match the menu values:
			// - BUY orders should compare against the value shown as “topSell” (~8489.1 area)
			// - SELL offers should compare against the value shown as “topBuy” (~10036.4 area)
			// Therefore we intentionally invert assignment here.
			double topBuyPrice = readFirstPrice(product.getAsJsonArray("sell_summary"));  // value used for BUY offers
			double topSellPrice = readFirstPrice(product.getAsJsonArray("buy_summary")); // value used for SELL offers



			products.put(productId, new BazaarProductData(productId, topBuyPrice, topSellPrice));


		}
	}

	/**
	 * Hypixel's bazaar endpoint provides ordered summaries.
	 * The first element represents the best/top price for that side.
	 */
	private static double readFirstPrice(JsonArray summary) {
		if (summary == null || summary.size() == 0) {
			return Double.NaN;
		}

		JsonObject first = summary.get(0).getAsJsonObject();
		return first.get("pricePerUnit").getAsDouble();
	}


	public Optional<BazaarProductData> getProduct(String productId) {
		return Optional.ofNullable(products.get(productId));
	}

	public Map<String, BazaarProductData> getProducts() {
		return Collections.unmodifiableMap(products);
	}

	public long getLastFetchMillis() {
		return lastFetchMillis;
	}

	public boolean hasData() {
		return !products.isEmpty();
	}
}
