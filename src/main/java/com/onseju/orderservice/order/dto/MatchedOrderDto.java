package com.onseju.orderservice.order.dto;

import java.math.BigDecimal;

public record MatchedOrderDto(
        Long buyOrderId,
        Long sellOrderId,
        BigDecimal quantity
) {
}
