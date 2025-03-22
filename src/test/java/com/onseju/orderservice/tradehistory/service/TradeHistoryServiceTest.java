package com.onseju.orderservice.tradehistory.service;

import com.onseju.orderservice.fake.FakeTradeHistoryRepository;
import com.onseju.orderservice.tradehistory.domain.TradeHistory;
import com.onseju.orderservice.tradehistory.dto.TradeHistoryResponse;
import com.onseju.orderservice.tradehistory.mapper.TradeHistoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TradeHistoryServiceTest {

	private FakeTradeHistoryRepository tradeHistoryRepository;
	private TradeHistoryMapper tradeHistoryMapper = new TradeHistoryMapper();
	private TradeHistoryService tradeHistoryService;

	@BeforeEach
	void setUp() {
		tradeHistoryRepository = new FakeTradeHistoryRepository();
		tradeHistoryService = new TradeHistoryService(tradeHistoryRepository, tradeHistoryMapper);
	}

	@Test
	@DisplayName("체결 내역을 저장한다.")
	void save() {
	    // given
		TradeHistoryResponse tradeHistoryResponse = new TradeHistoryResponse(
				"005930",
				1L,
				2L,
				new BigDecimal(100),
				new BigDecimal(1000),
				LocalDateTime.of(2025, 01, 01, 0, 0).toEpochSecond(ZoneOffset.UTC));

		// when
		tradeHistoryService.saveTradeHistory(tradeHistoryResponse);

	    // then
		TradeHistory saved = tradeHistoryRepository.findById(1L);
		assertThat(saved).isNotNull();
		assertThat(saved.getCompanyCode()).isEqualTo(tradeHistoryResponse.companyCode());
		assertThat(saved.getSellOrderId()).isEqualTo(tradeHistoryResponse.sellOrderId());
		assertThat(saved.getBuyOrderId()).isEqualTo(tradeHistoryResponse.buyOrderId());
		assertThat(saved.getTradeTime()).isEqualTo(tradeHistoryResponse.tradeTime());
	}
}
