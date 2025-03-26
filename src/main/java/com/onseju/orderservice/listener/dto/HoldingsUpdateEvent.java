package com.onseju.orderservice.listener.dto;

import java.math.BigDecimal;
import java.util.UUID;


public record HoldingsUpdateEvent(
		UUID id,
		Long holdingsId,
		String companyCode,
		BigDecimal quantity,
		BigDecimal reservedQuantity,
		BigDecimal averagePrice,
		BigDecimal totalPurchasePrice
) {
}
