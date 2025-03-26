package com.onseju.orderservice.holding.domain;

import com.onseju.orderservice.global.entity.BaseEntity;
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

	@Column(nullable = false, insertable = false, updatable = false)
	private String companyCode;

	@Column(nullable = false, insertable = false, updatable = false)
	private BigDecimal quantity;

	// 앞으로 거래될 예정인 주식의 수
	@Column(nullable = false, insertable = false, updatable = false)
	private BigDecimal reservedQuantity;

	@Column(nullable = false, insertable = false, updatable = false)
	private BigDecimal averagePrice;

	@Column(nullable = false, insertable = false, updatable = false)
	private BigDecimal totalPurchasePrice;

	@Column(nullable = false)
	private Long accountId;




}
