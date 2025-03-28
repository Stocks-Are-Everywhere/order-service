package com.onseju.orderservice.order.dto;

import lombok.Builder;

@Builder
public record OrderValidationResponse(Long accountId, Boolean result) {
}
