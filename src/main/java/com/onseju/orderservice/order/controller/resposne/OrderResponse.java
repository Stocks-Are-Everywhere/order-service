package com.onseju.orderservice.order.controller.resposne;

import java.math.BigDecimal;
import java.util.UUID;

import com.onseju.orderservice.order.domain.Type;

import lombok.Builder;

@Builder
public record OrderResponse (
		Long id,
		String companyCode,
		Type type,
		BigDecimal totalQuantity,
		BigDecimal price
) {
}