package com.onseju.orderservice.order.domain;

import com.onseju.orderservice.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Slf4j
@Table(name = "orders")
public class Order extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "order_id")
	private Long id;

	@Column(nullable = false)
	private String companyCode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Type type;

	@Column(nullable = false, precision = 10)
	private BigDecimal totalQuantity;

	@Column(nullable = false, precision = 10)
	private BigDecimal remainingQuantity;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private OrderStatus status;

	@Column(nullable = false, precision = 10)
	private BigDecimal price;

	@JoinColumn(nullable = false)
	private Long accountId;

	@Column(nullable = false)
	private Long timestamp;

	// 체결 처리 (수량 감소 및 상태 업데이트)
	public void decreaseRemainingQuantity(final BigDecimal matchedQuantity) {
		this.remainingQuantity = remainingQuantity.subtract(matchedQuantity);
		updateStatusBasedOnQuantity();
	}

	// 주문 수량 변경에 따른 상태 업데이트
	private void updateStatusBasedOnQuantity() {
		if (this.remainingQuantity.equals(BigDecimal.ZERO)) {
			this.status = OrderStatus.COMPLETE;
		}
	}
}
