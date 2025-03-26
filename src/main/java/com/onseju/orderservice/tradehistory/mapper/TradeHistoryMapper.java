package com.onseju.orderservice.tradehistory.mapper;

import com.onseju.orderservice.tradehistory.domain.TradeHistory;
import com.onseju.orderservice.tradehistory.dto.TradeHistoryDto;
import org.springframework.stereotype.Component;

@Component
public class TradeHistoryMapper {

    public TradeHistory toEntity(final TradeHistoryDto dto) {
        return TradeHistory.builder()
                .companyCode(dto.companyCode())
                .sellOrderId(dto.sellOrderId())
                .buyOrderId(dto.buyOrderId())
                .price(dto.price())
                .quantity(dto.quantity())
                .tradeTime(dto.tradeTime())
                .build();
    }
}
