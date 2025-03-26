package com.onseju.orderservice.order.dto;

import java.math.BigDecimal;

/**
 * 호가창의 가격 레벨 데이터 (가격별 주문 정보)
 */
public record PriceLevelDto(
		BigDecimal price,      // 가격
		BigDecimal quantity,   // 수량 합계
		Integer orderCount     // 주문 건수
) {
}
