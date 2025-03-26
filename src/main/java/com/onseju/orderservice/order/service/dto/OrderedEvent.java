package com.onseju.orderservice.order.service.dto;

import com.onseju.orderservice.order.domain.OrderStatus;
import com.onseju.orderservice.order.domain.Type;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderedEvent(
		UUID id,
        Long orderId,
        String companyCode,
        Type type,
        OrderStatus status,
        BigDecimal totalQuantity,
        BigDecimal remainingQuantity,
        BigDecimal price,
		Long timestamp,
        Long accountId
) {
}
