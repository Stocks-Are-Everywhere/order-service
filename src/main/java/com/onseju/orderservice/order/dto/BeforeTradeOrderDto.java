package com.onseju.orderservice.order.dto;

import com.onseju.orderservice.order.domain.Type;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record BeforeTradeOrderDto(
		String companyCode,
		Type type,
		BigDecimal totalQuantity,
		BigDecimal price,
		Long memberId
) {
}
