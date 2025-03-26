package com.onseju.orderservice.listener.dto;

import java.math.BigDecimal;

public record MatchedEvent(
        String companyCode,
        Long buyOrderId,
        Long sellOrderId,
        BigDecimal quantity,
        BigDecimal price,
        Long tradeAt
) {
}
