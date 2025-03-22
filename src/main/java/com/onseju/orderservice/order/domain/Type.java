package com.onseju.orderservice.order.domain;

public enum Type {

	LIMIT_SELL,
	LIMIT_BUY,
	MARKET_SELL,
	MARKET_BUY;

	public boolean isSell() {
		return this == LIMIT_SELL || this == MARKET_SELL;
	}
}
