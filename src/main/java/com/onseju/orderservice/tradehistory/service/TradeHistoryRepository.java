package com.onseju.orderservice.tradehistory.service;

import com.onseju.orderservice.tradehistory.domain.TradeHistory;

public interface TradeHistoryRepository {

    TradeHistory save(final TradeHistory tradeHistory);
}
