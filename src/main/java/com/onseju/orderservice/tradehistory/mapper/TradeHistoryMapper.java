package com.onseju.orderservice.tradehistory.mapper;

import org.springframework.stereotype.Component;

import com.onseju.orderservice.events.MatchedEvent;
import com.onseju.orderservice.tradehistory.domain.TradeHistory;

@Component
public class TradeHistoryMapper {

	public TradeHistory toEntity(final MatchedEvent event) {
		return TradeHistory.builder()
				.companyCode(event.companyCode())
				.sellOrderId(event.sellOrderId())
				.buyOrderId(event.buyOrderId())
				.price(event.price())
				.quantity(event.quantity())
				.tradeTime(event.tradeAt())
				.build();
	}
}
