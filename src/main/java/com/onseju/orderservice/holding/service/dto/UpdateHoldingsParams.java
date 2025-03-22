package com.onseju.orderservice.holding.service.dto;

import com.onseju.orderservice.order.domain.Type;

import java.math.BigDecimal;

public record UpdateHoldingsParams(
        Type type,
        Long accountId,
        String companyCode,
        BigDecimal price,
        BigDecimal quantity
) {
}
