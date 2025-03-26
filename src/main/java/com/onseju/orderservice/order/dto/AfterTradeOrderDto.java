package com.onseju.orderservice.order.dto;

import java.math.BigDecimal;

import lombok.Builder;

@Builder
public record AfterTradeOrderDto(
		Long orderId,
		BigDecimal quantity
) {
}
