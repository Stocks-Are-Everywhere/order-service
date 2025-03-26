package com.onseju.orderservice.listener;

import com.onseju.orderservice.order.dto.MatchedOrderDto;
import com.onseju.orderservice.tradehistory.dto.TradeHistoryDto;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

    public TradeHistoryDto toTradeHistoryDto(final MatchedEvent matchedEvent) {
        return new TradeHistoryDto(
                matchedEvent.companyCode(),
                matchedEvent.sellOrderId(),
                matchedEvent.buyOrderId(),
                matchedEvent.quantity(),
                matchedEvent.price(),
                matchedEvent.tradeAt()
        );
    }

    public MatchedOrderDto toMatchedOrderDto(final MatchedEvent matchedEvent) {
        return new MatchedOrderDto(
                matchedEvent.buyOrderId(),
                matchedEvent.sellOrderId(),
                matchedEvent.quantity()
        );
    }
}
