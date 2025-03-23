package com.onseju.orderservice.listener;

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
}
