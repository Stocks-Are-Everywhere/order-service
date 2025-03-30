package com.onseju.orderservice.tradehistory.service;

import com.onseju.orderservice.fake.FakeOrderRepository;
import com.onseju.orderservice.fake.FakeTradeHistoryRepository;
import com.onseju.orderservice.tradehistory.domain.TradeHistory;
import com.onseju.orderservice.tradehistory.mapper.TradeHistoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TradeHistoryServiceTest {

	private FakeTradeHistoryRepository tradeHistoryRepository;
	private TradeHistoryService tradeHistoryService;
	TradeHistoryMapper tradeHistoryMapper;
	FakeOrderRepository orderRepository;
	@BeforeEach
	void setUp() {
		tradeHistoryRepository = new FakeTradeHistoryRepository();
		tradeHistoryMapper = new TradeHistoryMapper();
		orderRepository = new FakeOrderRepository();
		tradeHistoryService = new TradeHistoryService(tradeHistoryRepository, orderRepository, tradeHistoryMapper);
	}

	@Test
	@DisplayName("체결 내역을 저장한다.")
	void save() {
		// given
		TradeHistory tradeHistory = TradeHistory.builder()
			.id(1L)
			.companyCode("005930")
			.sellOrderId(1L)
			.buyOrderId(2L)
			.price(new BigDecimal(100))
			.quantity(new BigDecimal(100))
			.tradeTime(Instant.now().getEpochSecond())
			.build();

		// when
		tradeHistoryService.saveTradeHistory(tradeHistory);

		// then
		TradeHistory saved = tradeHistoryRepository.findById(1L).orElse(null);
		assertThat(saved).isNotNull();
		assertThat(saved.getCompanyCode()).isEqualTo(tradeHistory.getCompanyCode());
		assertThat(saved.getSellOrderId()).isEqualTo(tradeHistory.getSellOrderId());
		assertThat(saved.getBuyOrderId()).isEqualTo(tradeHistory.getBuyOrderId());
		assertThat(saved.getTradeTime()).isEqualTo(tradeHistory.getTradeTime());
	}
}
