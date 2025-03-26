package com.onseju.orderservice.order.controller.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.onseju.orderservice.order.domain.Type;

import lombok.Builder;

@Builder
public record OrderResponse(
		Long id,
		String companyCode,
		Type type
) {

}
