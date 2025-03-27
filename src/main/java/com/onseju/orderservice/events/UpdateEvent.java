package com.onseju.orderservice.events;

import java.math.BigDecimal;

import lombok.Builder;

@Builder
public record UpdateEvent(
		String companyCode,
		Long buyOrderId,
		Long buyAccountId,
		Long sellOrderId,
		Long sellAccountId,
		BigDecimal quantity,
		BigDecimal price,
		Long tradeAt
) {
}
