package com.onseju.orderservice.tradehistory.service;

import com.onseju.orderservice.tradehistory.domain.TradeHistory;

public interface TradeHistoryRepository {

    TradeHistory save(final TradeHistory tradeHistory);
    void deleteAll();
    TradeHistory findByBuyOrderIdAndSellOrderId(final Long buyOrderId, final Long sellOrderId);
}
