package com.onseju.orderservice.order.service.dto;

import com.onseju.orderservice.order.domain.OrderStatus;
import com.onseju.orderservice.order.domain.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderedEvent(
        Long id,
        String companyCode,
        Type type,
        OrderStatus status,
        BigDecimal totalQuantity,
        BigDecimal remainingQuantity,
        BigDecimal price,
        LocalDateTime createdDateTime,
        Long accountId
) {
}
