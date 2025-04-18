package com.onseju.orderservice.order.domain;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

import com.onseju.orderservice.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Slf4j
@Table(name = "orders")
public class Order extends BaseEntity {

	@Id
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

	@Transient
	private AtomicReference<BigDecimal> atomicRemainingQuantity;

	@PostLoad
	public void initializeAtomicReference() {
		if (atomicRemainingQuantity == null) {
			atomicRemainingQuantity = new AtomicReference<>(remainingQuantity);
		}
	}

	// 체결 처리 (수량 감소 및 상태 업데이트)
	public void decreaseRemainingQuantity(final BigDecimal quantity) {
		// AtomicReference가 초기화되지 않았으면 초기화
		if (atomicRemainingQuantity == null) {
			initializeAtomicReference();
		}

		BigDecimal current, updated;
		do {
			current = atomicRemainingQuantity.get();
			updated = current.subtract(quantity);
			if (updated.compareTo(BigDecimal.ZERO) < 0) {
				updated = BigDecimal.ZERO;
			}
		} while (!atomicRemainingQuantity.compareAndSet(current, updated));

		// DB 저장용 필드도 업데이트
		this.remainingQuantity = updated;

		updateStatusBasedOnQuantity();
	}

	// 주문 수량 변경에 따른 상태 업데이트
	private void updateStatusBasedOnQuantity() {
		if (this.remainingQuantity.compareTo(BigDecimal.ZERO) == 0) {
			this.status = OrderStatus.COMPLETE;
		}
	}
}
