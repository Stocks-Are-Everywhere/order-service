package com.onseju.orderservice.tradehistory.dto;

import com.onseju.orderservice.order.domain.Type;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record TradeHistoryResponse(
		Long orderId,
		String companyCode,
		Type type,
		BigDecimal price,
		BigDecimal quantity,
		LocalDateTime createdAt
) {
}
