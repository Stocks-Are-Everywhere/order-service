package com.onseju.orderservice.order.service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MatchedEvent(
		UUID id,
        String companyCode,
        Long buyOrderId,
        Long sellOrderId,
        BigDecimal quantity,
        BigDecimal price,
        Long tradeAt
) {
}
