package com.onseju.orderservice.tradehistory.mapper;

import com.onseju.orderservice.events.MatchedEvent;
import com.onseju.orderservice.order.domain.Order;
import com.onseju.orderservice.tradehistory.domain.TradeHistory;
import com.onseju.orderservice.tradehistory.dto.TradeHistoryResponse;
import org.springframework.stereotype.Component;

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

	public TradeHistoryResponse toResponse(final TradeHistory tradeHistory, final Order order) {
		return new TradeHistoryResponse(
				order.getId(),
				order.getCompanyCode(),
				order.getType(),
				tradeHistory.getPrice(),
				tradeHistory.getQuantity(),
				tradeHistory.getCreatedDateTime()
		);
	}
}
