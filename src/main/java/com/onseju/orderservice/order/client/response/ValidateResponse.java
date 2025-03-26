package com.onseju.orderservice.order.client.response;

import com.onseju.orderservice.order.domain.Type;

public record ValidateResponse(boolean valid, String message, Long accountId, Type type) {
}
