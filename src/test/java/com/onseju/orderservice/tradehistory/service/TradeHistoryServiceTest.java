package com.onseju.orderservice.tradehistory.service;

import com.onseju.orderservice.fake.FakeTradeHistoryRepository;
import com.onseju.orderservice.tradehistory.domain.TradeHistory;
import com.onseju.orderservice.tradehistory.dto.TradeHistoryDto;
import com.onseju.orderservice.tradehistory.mapper.TradeHistoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TradeHistoryServiceTest {

	private TradeHistoryService tradeHistoryService;
	private FakeTradeHistoryRepository tradeHistoryRepository;
	private final TradeHistoryMapper tradeHistoryMapper = new TradeHistoryMapper();

	@BeforeEach
	void setUp() {
		tradeHistoryRepository = new FakeTradeHistoryRepository();
		tradeHistoryService = new TradeHistoryService(tradeHistoryRepository, tradeHistoryMapper);
	}

	@Test
	@DisplayName("체결 내역을 저장한다.")
	void save() {
	    // given
		TradeHistoryDto tradeHistoryDto = new TradeHistoryDto(
				"005930",
				1L,
				2L,
				new BigDecimal(100),
				new BigDecimal(1000),
				LocalDateTime.of(2025, 01, 01, 0, 0).toEpochSecond(ZoneOffset.UTC));

		// when
		tradeHistoryService.save(tradeHistoryDto);

	    // then
		TradeHistory saved = tradeHistoryRepository.findById(1L);
		assertThat(saved).isNotNull();
		assertThat(saved.getCompanyCode()).isEqualTo(tradeHistoryDto.companyCode());
		assertThat(saved.getSellOrderId()).isEqualTo(tradeHistoryDto.sellOrderId());
		assertThat(saved.getBuyOrderId()).isEqualTo(tradeHistoryDto.buyOrderId());
		assertThat(saved.getTradeTime()).isEqualTo(tradeHistoryDto.tradeTime());
	}
}
