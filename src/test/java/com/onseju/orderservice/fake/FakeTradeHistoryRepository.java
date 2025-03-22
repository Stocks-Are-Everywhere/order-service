package com.onseju.orderservice.fake;

import com.onseju.orderservice.tradehistory.domain.TradeHistory;
import com.onseju.orderservice.tradehistory.service.TradeHistoryRepository;

import java.util.Comparator;
import java.util.concurrent.ConcurrentSkipListSet;

public class FakeTradeHistoryRepository implements TradeHistoryRepository {

    private final ConcurrentSkipListSet<TradeHistory> elements = new ConcurrentSkipListSet<>(Comparator.comparing(TradeHistory::getId));

    @Override
    public TradeHistory save(TradeHistory tradeHistory) {
        if (hasElement(tradeHistory)) {
            elements.stream()
                    .filter(element -> element.getId().equals(tradeHistory.getId()))
                    .forEach(elements::remove);
            elements.add(tradeHistory);
            return tradeHistory;
        }
        TradeHistory saved = TradeHistory.builder()
                .id((long) (elements.size() + 1))
                .companyCode(tradeHistory.getCompanyCode())
                .sellOrderId(tradeHistory.getSellOrderId())
                .buyOrderId(tradeHistory.getBuyOrderId())
                .price(tradeHistory.getPrice())
                .quantity(tradeHistory.getQuantity())
                .tradeTime(tradeHistory.getTradeTime())
                .build();
        elements.add(saved);
        return saved;
    }

    private boolean hasElement(TradeHistory tradeHistory) {
        if (tradeHistory.getId() == null) {
            return false;
        }
        return elements.stream()
                .anyMatch(o -> o.getId().equals(tradeHistory.getId()));
    }

    public TradeHistory findById(long id) {
        return elements.stream()
                .filter(o -> o.getId().equals(id))
                .findAny()
                .orElse(null);
    }
}
