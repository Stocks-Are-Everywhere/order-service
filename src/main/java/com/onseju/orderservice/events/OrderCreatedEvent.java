package com.onseju.orderservice.events;

import java.math.BigDecimal;

import com.onseju.orderservice.order.domain.OrderStatus;
import com.onseju.orderservice.order.domain.Type;

import lombok.Builder;

@Builder
public record OrderCreatedEvent(
		Long id,
		String companyCode,
		Type type,
		OrderStatus status,
		BigDecimal totalQuantity,
		BigDecimal remainingQuantity,
		BigDecimal price,
		Long timestamp,
		Long accountId
) {
}
