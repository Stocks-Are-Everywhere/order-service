package com.onseju.orderservice.tradehistory.domain;

import java.math.BigDecimal;

import com.onseju.orderservice.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Slf4j
public class TradeHistory extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "trade_history_id")
	private Long id;

	@Column(nullable = false)
	private String companyCode;

	@Column(nullable = false)
	private Long sellOrderId;

	@Column(nullable = false)
	private Long buyOrderId;

	@Column(nullable = false)
	private BigDecimal price;    // 체결 가격

	@Column(nullable = false)
	private BigDecimal quantity; // 체결 수량

	@Column(nullable = false)
	private Long tradeTime; // 체결 시간

}
