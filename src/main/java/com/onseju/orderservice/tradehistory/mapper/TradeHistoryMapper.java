package com.onseju.orderservice.tradehistory.mapper;

import com.onseju.orderservice.tradehistory.domain.TradeHistory;
import com.onseju.orderservice.tradehistory.dto.TradeHistoryResponse;
import org.springframework.stereotype.Component;

@Component
public class TradeHistoryMapper {

    public TradeHistory toEntity(final TradeHistoryResponse tradeHistoryResponse) {
        return TradeHistory.builder()
                .companyCode(tradeHistoryResponse.companyCode())
                .sellOrderId(tradeHistoryResponse.sellOrderId())
                .buyOrderId(tradeHistoryResponse.buyOrderId())
                .price(tradeHistoryResponse.price())
                .quantity(tradeHistoryResponse.quantity())
                .tradeTime(tradeHistoryResponse.tradeTime())
                .build();
    }
}
