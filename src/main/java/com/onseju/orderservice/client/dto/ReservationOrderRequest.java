package com.onseju.orderservice.client.dto;

import com.onseju.orderservice.order.domain.Type;

import java.math.BigDecimal;

public record ReservationOrderRequest(
        Long memberId,
        String companyCode,
        Type type,
        BigDecimal price,
        BigDecimal quantity
) {
}
