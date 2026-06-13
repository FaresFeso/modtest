package com.bazaaralert.client.model;

import java.util.Objects;

public final class BazaarOrder {
	private final String productId;
	private final String displayName;
	private final OrderType type;
	private final double pricePerUnit;
	private final long amount;

	public BazaarOrder(String productId, String displayName, OrderType type, double pricePerUnit, long amount) {
		this.productId = productId;
		this.displayName = displayName;
		this.type = type;
		this.pricePerUnit = pricePerUnit;
		this.amount = amount;
	}

	public String productId() {
		return productId;
	}

	public String displayName() {
		return displayName;
	}

	public OrderType type() {
		return type;
	}

	public double pricePerUnit() {
		return pricePerUnit;
	}

	public long amount() {
		return amount;
	}

	public String key() {
		return productId + ":" + type.name();
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof BazaarOrder other)) {
			return false;
		}
		return Objects.equals(key(), other.key());
	}

	@Override
	public int hashCode() {
		return Objects.hash(key());
	}

	@Override
	public String toString() {
		return type + " " + displayName + " @ " + pricePerUnit + " coins";
	}
}
