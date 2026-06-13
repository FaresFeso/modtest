package com.bazaaralert.client.tracker;

import com.bazaaralert.client.model.BazaarOrder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OrderTracker {
	private final Map<String, BazaarOrder> orders = new LinkedHashMap<>();

	public void replaceAll(Collection<BazaarOrder> newOrders) {
		orders.clear();
		for (BazaarOrder order : newOrders) {
			orders.put(order.key(), order);
		}
	}

	public void upsert(BazaarOrder order) {
		orders.put(order.key(), order);
	}

	public void remove(String key) {
		orders.remove(key);
	}

	public void clear() {
		orders.clear();
	}

	public List<BazaarOrder> getOrders() {
		return new ArrayList<>(orders.values());
	}

	public boolean isEmpty() {
		return orders.isEmpty();
	}

	public int size() {
		return orders.size();
	}
}
