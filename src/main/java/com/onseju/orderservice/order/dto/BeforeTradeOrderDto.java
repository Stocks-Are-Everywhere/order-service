package com.onseju.orderservice.order.dto;

import java.math.BigDecimal;

import com.onseju.orderservice.order.domain.Type;

import lombok.Builder;

@Builder
public record BeforeTradeOrderDto(
		String companyCode,
		Type type,
		BigDecimal totalQuantity,
		BigDecimal price,
		Long accountId
) {
}
