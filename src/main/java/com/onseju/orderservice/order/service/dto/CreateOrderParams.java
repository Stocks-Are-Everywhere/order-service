package com.onseju.orderservice.order.service.dto;

import com.onseju.orderservice.order.domain.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateOrderParams(
        String companyCode,
        Type type,
        BigDecimal totalQuantity,
        BigDecimal price,
        LocalDateTime now,
        Long memberId
) {
}
