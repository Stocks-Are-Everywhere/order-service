package com.onseju.orderservice.tradehistory.dto;

import com.onseju.orderservice.order.domain.Type;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record MatchingNotificationDto(
        Long orderId,
        String companyCode,
        Type type,
        BigDecimal price,
        BigDecimal quantity,
        Long createdAt
) {
}
