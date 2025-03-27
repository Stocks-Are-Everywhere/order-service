package com.onseju.orderservice.tradehistory.mapper;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.onseju.orderservice.events.mapper.TradeHistoryMapper;
import com.onseju.orderservice.tradehistory.domain.TradeHistory;
import com.onseju.orderservice.tradehistory.dto.TradeHistoryResponse;

class TradeHistoryMapperTest {

	private final TradeHistoryMapper mapper = new TradeHistoryMapper();

	@Test
	@DisplayName("체결관련 dto를 TradeHistory 엔티티로 변환한다.")
	void toEntity() {
		// given
		TradeHistoryResponse tradeHistoryResponse = new TradeHistoryResponse(
				"005930",
				1L,
				2L,
				new BigDecimal(100),
				new BigDecimal(1000),
				LocalDateTime.of(2025, 01, 01, 0, 0).toEpochSecond(ZoneOffset.UTC));

		// when
		TradeHistory tradeHistory = mapper.toEntity(tradeHistoryResponse);

		// then
		assertThat(tradeHistory).isNotNull();
		assertThat(tradeHistory.getCompanyCode()).isEqualTo(tradeHistoryResponse.companyCode());
	}
}
