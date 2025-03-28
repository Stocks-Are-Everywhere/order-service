package com.onseju.orderservice.tradehistory.mapper;

import com.onseju.orderservice.events.MatchedEvent;
import com.onseju.orderservice.tradehistory.domain.TradeHistory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TradeHistoryMapperTest {

	private final TradeHistoryMapper mapper = new TradeHistoryMapper();

	@Test
	@DisplayName("체결관련 dto를 TradeHistory 엔티티로 변환한다.")
	void toEntity() {
		// given
		MatchedEvent event = new MatchedEvent(
				UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479"),
				"005930",
				1L,
				1L,
				2L,
				2L,
				new BigDecimal(100),
				new BigDecimal(1000),
				LocalDateTime.of(2025, 01, 01, 0, 0).toEpochSecond(ZoneOffset.UTC));

		// when
		TradeHistory tradeHistory = mapper.toEntity(event);

		// then
		assertThat(tradeHistory).isNotNull();
		assertThat(tradeHistory.getCompanyCode()).isEqualTo(event.companyCode());
	}
}
