package com.onseju.orderservice.holding.domain;

import com.onseju.orderservice.global.entity.BaseEntity;
import com.onseju.orderservice.order.domain.Type;
import com.onseju.orderservice.holding.exception.HoldingsNotFoundException;
import com.onseju.orderservice.holding.exception.InsufficientHoldingsException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Slf4j
@Table(name = "holdings",
		uniqueConstraints = {
				@UniqueConstraint(
						name = "uk_account_company",
						columnNames = {"account_id", "company_code"}
				)
		})
public class Holdings extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, updatable = false)
	private String companyCode;

	@Column(nullable = false)
	private BigDecimal quantity;

	// 앞으로 거래될 예정인 주식의 수
	@Column(nullable = false)
	private BigDecimal reservedQuantity;

	@Column(nullable = false)
	private BigDecimal averagePrice;

	@Column(nullable = false)
	private BigDecimal totalPurchasePrice;

	@Column(nullable = false)
	private Long accountId;

	public void validateEnoughHoldings(final BigDecimal checkQuantity) {
		if (getAvailableQuantity().compareTo(checkQuantity) < 0) {
			throw new InsufficientHoldingsException();
		}
	}

	public void validateExistHoldings() {
		if (this.quantity.equals(BigDecimal.ZERO)) {
			throw new HoldingsNotFoundException();
		}
	}

	// 예약 주문 처리
	public void reserveOrder(final BigDecimal reservedQuantity) {
		this.reservedQuantity = this.reservedQuantity.add(reservedQuantity);
	}

	public void updateHoldings(final Type type, final BigDecimal updatePrice, final BigDecimal updateQuantity) {
		if (type.isSell()) {
			updateSellHoldings(updateQuantity);
		} else {
			updateBuyHoldings(updatePrice, updateQuantity);
		}
	}

	private BigDecimal getAvailableQuantity() {
		return this.quantity.subtract(this.reservedQuantity);
	}

	private void updateBuyHoldings(final BigDecimal updatePrice, final BigDecimal updateQuantity) {
		this.quantity = this.quantity.add(updateQuantity);
		this.totalPurchasePrice = this.totalPurchasePrice.add(updateQuantity.multiply(updatePrice));
		this.averagePrice = this.totalPurchasePrice.divide(this.quantity, 4, RoundingMode.HALF_UP);
	}

	// 새로운 총 매수 금액 = 기존 총 매수 금액 − (평단가×매도 수량)
	// 손익 = (매도가 - 평단가) × 매도 수량
	private void updateSellHoldings(final BigDecimal updateQuantity) {
		this.quantity = this.quantity.subtract(updateQuantity);
		this.totalPurchasePrice = this.totalPurchasePrice.subtract(updateQuantity.multiply(this.averagePrice));

		// 예약 수량 감소 (체결된 만큼 예약 수량에서 제거)
		this.reservedQuantity = this.reservedQuantity.subtract(updateQuantity);

		if (this.quantity.equals(BigDecimal.ZERO)) {
			this.softDelete(LocalDateTime.now());
		}
	}
}
