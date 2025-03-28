package com.onseju.orderservice.tradehistory.repository;



import com.onseju.orderservice.global.entity.BaseEntity;
import com.onseju.orderservice.tradehistory.domain.TradeHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@DataJpaTest
class TradeHistoryJpaRepositoryTest {

	@Autowired
	private TradeHistoryJpaRepository tradeHistoryJpaRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@BeforeEach
	void setUp() {
		// 테스트 데이터 생성
		TradeHistory trade1 = TradeHistory.builder()
			.companyCode("COMP1")
			.sellOrderId(1L)
			.buyOrderId(2L)
			.price(new BigDecimal("100.00"))
			.quantity(new BigDecimal("10"))
			.tradeTime(System.currentTimeMillis())
			.build();
		// createdDateTime 설정
		setCreatedAndUpdatedDateTime(trade1);

		TradeHistory trade2 = TradeHistory.builder()
			.companyCode("COMP2")
			.sellOrderId(3L)
			.buyOrderId(4L)
			.price(new BigDecimal("200.00"))
			.quantity(new BigDecimal("5"))
			.tradeTime(System.currentTimeMillis())
			.build();
		// createdDateTime 설정
		setCreatedAndUpdatedDateTime(trade2);

		TradeHistory trade3 = TradeHistory.builder()
			.companyCode("COMP1")
			.sellOrderId(5L)
			.buyOrderId(6L)
			.price(new BigDecimal("150.00"))
			.quantity(new BigDecimal("7"))
			.tradeTime(System.currentTimeMillis())
			.build();
		// createdDateTime 설정
		setCreatedAndUpdatedDateTime(trade3);

		tradeHistoryJpaRepository.saveAll(Arrays.asList(trade1, trade2, trade3));

		// 영속성 컨텍스트 초기화
		entityManager.flush();
		entityManager.clear();
	}

	// BaseEntity의 필드를 리플렉션을 사용하여 설정
	private void setCreatedAndUpdatedDateTime(TradeHistory tradeHistory) {
		try {
			Field createdDateTimeField = BaseEntity.class.getDeclaredField("createdDateTime");
			createdDateTimeField.setAccessible(true);
			createdDateTimeField.set(tradeHistory, LocalDateTime.now());

			Field updatedDateTimeField = BaseEntity.class.getDeclaredField("updatedDateTime");
			updatedDateTimeField.setAccessible(true);
			updatedDateTimeField.set(tradeHistory, LocalDateTime.now());
		} catch (Exception e) {
			throw new RuntimeException("Failed to set created/updated date time", e);
		}
	}

	// 기존 테스트 메서드들...

	@Test
	void insert_ShouldInsertNewTradeHistory() {
		TradeHistory newTrade = TradeHistory.builder()
			.companyCode("COMP3")
			.sellOrderId(7L)
			.buyOrderId(8L)
			.price(new BigDecimal("300.00"))
			.quantity(new BigDecimal("3"))
			.tradeTime(System.currentTimeMillis())
			.build();
		// createdDateTime 설정
		setCreatedAndUpdatedDateTime(newTrade);

		TradeHistory savedTrade = tradeHistoryJpaRepository.save(newTrade);
		assertThat(savedTrade.getId()).isNotNull();
		assertThat(savedTrade.getCompanyCode()).isEqualTo("COMP3");

		List<String> companyCodes = tradeHistoryJpaRepository.findDistinctCompanyCodes();
		assertThat(companyCodes).contains("COMP3");
	}
}
