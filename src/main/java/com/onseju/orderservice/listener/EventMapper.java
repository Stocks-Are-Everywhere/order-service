package com.onseju.orderservice.listener;

import com.onseju.orderservice.holding.service.dto.UpdateHoldingsParams;
import com.onseju.orderservice.order.domain.Order;
import com.onseju.orderservice.tradehistory.domain.TradeHistory;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

    public TradeHistory toTradeHistory(final MatchedEvent matchedEvent) {
        return TradeHistory.builder()
                .companyCode(matchedEvent.companyCode())
                .sellOrderId(matchedEvent.sellOrderId())
                .buyOrderId(matchedEvent.buyOrderId())
                .price(matchedEvent.price())
                .quantity(matchedEvent.quantity())
                .tradeTime(matchedEvent.tradeAt())
                .build();
    }

    public UpdateHoldingsParams toUpdateHoldingsParams(final Order order, final MatchedEvent matchedEvent) {
        return new UpdateHoldingsParams(
                order.getType(),
                order.getAccountId(),
                order.getCompanyCode(),
                matchedEvent.price(),
                matchedEvent.quantity()
        );
    }
}
