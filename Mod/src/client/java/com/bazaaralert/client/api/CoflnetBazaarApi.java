package com.bazaaralert.client.api;

import com.bazaaralert.client.BazaarAlert;
import com.google.gson.*;
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

/**
 * Coflnet bazaar price feed.
 *
 * Uses:
 * - GET https://sky.coflnet.com/api/flip/bazaar/spread
 *
 * JSON shape (based on user-provided sample):
 *   [{ flip: { itemTag, buyPrice, sellPrice, ... }, itemName, isManipulated }, ...]
 */
public final class CoflnetBazaarApi {
	private static final Logger LOGGER = LoggerFactory.getLogger(BazaarAlert.MOD_ID);
	private static final URI SPREAD_URI = URI.create("https://sky.coflnet.com/api/flip/bazaar/spread");

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
		HttpRequest request = HttpRequest.newBuilder(SPREAD_URI)
			.timeout(Duration.ofSeconds(20))
			.header("User-Agent", "BazaarAlert/1.0")
			.GET()
			.build();

		return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenApply(response -> {
				if (response.statusCode() != 200) {
					LOGGER.warn("Coflnet bazaar API returned status {}", response.statusCode());
					return false;
				}

				try {
					JsonElement parsed = JsonParser.parseString(response.body());
					if (!parsed.isJsonArray()) {
						LOGGER.warn("Coflnet bazaar API returned non-array payload");
						return false;
					}

					parseSpread(parsed.getAsJsonArray());
					lastFetchMillis = System.currentTimeMillis();
					return !products.isEmpty();
				} catch (RuntimeException e) {
					LOGGER.warn("Failed to parse Coflnet bazaar API response", e);
					return false;
				}
			})
			.whenComplete((ignored, throwable) -> fetchInProgress = false);
	}

	private void parseSpread(JsonArray spreadArray) {
		products.clear();

		for (JsonElement element : spreadArray) {
			if (!element.isJsonObject()) {
				continue;
			}

			JsonObject root = element.getAsJsonObject();
			JsonObject flip = getAsObjectOrNull(root, "flip");
			if (flip == null) {
				continue;
			}

			String itemTag = getAsStringOrNull(flip, "itemTag");
			if (itemTag == null || itemTag.isEmpty()) {
				continue;
			}

			Double buyPrice = getAsDoubleOrNull(flip, "buyPrice");
			Double sellPrice = getAsDoubleOrNull(flip, "sellPrice");
			if (buyPrice == null || sellPrice == null) {
				continue;
			}

			// Per your mapping: 
			// - BUY orders compare against flip.buyPrice (top buy)
			// - SELL offers compare against flip.sellPrice (top sell)
			// We'll store as:
			//   topBuyPrice = buyPrice
			//   topSellPrice = sellPrice
			products.put(itemTag, new BazaarProductData(itemTag, buyPrice, sellPrice));
		}
	}

	private static JsonObject getAsObjectOrNull(JsonObject obj, String key) {
		JsonElement el = obj.get(key);
		if (el == null || !el.isJsonObject()) {
			return null;
		}
		return el.getAsJsonObject();
	}

	private static String getAsStringOrNull(JsonObject obj, String key) {
		JsonElement el = obj.get(key);
		if (el == null || el.isJsonNull()) {
			return null;
		}
		try {
			return el.getAsString();
		} catch (RuntimeException ignored) {
			return null;
		}
	}

	private static Double getAsDoubleOrNull(JsonObject obj, String key) {
		JsonElement el = obj.get(key);
		if (el == null || el.isJsonNull()) {
			return null;
		}
		try {
			return el.getAsDouble();
		} catch (RuntimeException ignored) {
			return null;
		}
	}

	public Optional<BazaarProductData> getProduct(String productId) {
		return Optional.ofNullable(products.get(productId));
	}

	public Map<String, BazaarProductData> getProducts() {
		return Collections.unmodifiableMap(products);
	}

	public boolean hasData() {
		return !products.isEmpty();
	}

	public long getLastFetchMillis() {
		return lastFetchMillis;
	}
}

