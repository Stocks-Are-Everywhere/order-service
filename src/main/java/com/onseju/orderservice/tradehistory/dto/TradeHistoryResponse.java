package com.onseju.orderservice.tradehistory.dto;

import java.math.BigDecimal;

public record TradeHistoryResponse(
		String companyCode,
		Long sellOrderId,
		Long buyOrderId,
		BigDecimal quantity,
		BigDecimal price,
		Long tradeTime
) {
}
