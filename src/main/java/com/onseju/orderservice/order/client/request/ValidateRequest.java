package com.onseju.orderservice.order.client.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.onseju.orderservice.order.domain.Account;
import com.onseju.orderservice.order.domain.Type;

public record ValidateRequest(String companyCode,
							  Type type,
							  BigDecimal totalQuantity,
							  BigDecimal price,
							  LocalDateTime now,
							  Long memberId
							  ) {
}
