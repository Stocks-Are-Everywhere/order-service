package com.onseju.orderservice.listener.dto;

import com.onseju.orderservice.order.domain.Order;
import com.onseju.orderservice.order.service.dto.OrderedEvent;
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

    public Order toOrder(final OrderedEvent orderedEvent) {
        return Order.builder()
                .id(orderedEvent.orderId())
                .companyCode(orderedEvent.companyCode())
                .type(orderedEvent.type())
                .price(orderedEvent.price())
                .totalQuantity(orderedEvent.totalQuantity())
                .remainingQuantity(orderedEvent.remainingQuantity())
                .status(orderedEvent.status())
                .accountId(orderedEvent.accountId())
                .timestamp(orderedEvent.timestamp())
                .build();
    }
}
