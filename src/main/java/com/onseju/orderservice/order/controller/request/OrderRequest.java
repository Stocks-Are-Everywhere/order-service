package com.onseju.orderservice.order.controller.request;

import com.onseju.orderservice.order.domain.Type;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record OrderRequest(
		String companyCode,
		Type type,
		BigDecimal totalQuantity,
		BigDecimal price,
		LocalDateTime now
) {

}
