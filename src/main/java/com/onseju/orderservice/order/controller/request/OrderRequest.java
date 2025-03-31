package com.onseju.orderservice.order.controller.request;

import java.math.BigDecimal;

import com.onseju.orderservice.order.domain.Type;

import lombok.Builder;

@Builder
public record OrderRequest(
		String companyCode,
		Type type,
		BigDecimal totalQuantity,
		BigDecimal price,
		Long memberId
) {
}
