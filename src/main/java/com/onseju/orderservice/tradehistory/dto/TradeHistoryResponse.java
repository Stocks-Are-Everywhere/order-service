package com.onseju.orderservice.tradehistory.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record TradeHistoryResponse(
		String companyCode,
		Long sellOrderId,
		Long buyOrderId,
		BigDecimal quantity,
		BigDecimal price,
		Long tradeTime
) {
}
