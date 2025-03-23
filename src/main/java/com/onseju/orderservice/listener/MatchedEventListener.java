package com.onseju.orderservice.listener;

import com.onseju.orderservice.order.domain.Order;
import com.onseju.orderservice.order.service.repository.OrderRepository;
import com.onseju.orderservice.tradehistory.service.TradeHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class MatchedEventListener {

    private final TradeHistoryRepository tradeHistoryRepository;
    private final OrderRepository orderRepository;
    private final EventMapper eventMapper;

    @Async
    @TransactionalEventListener
    public void createTradeHistoryEvent(final MatchedEvent matchedEvent) {
        // 1. 주문 내역 조회
        Order buyOrder = orderRepository.getById(matchedEvent.buyOrderId());
        Order sellOrder = orderRepository.getById(matchedEvent.sellOrderId());

        // 2. 체결 내역 저장
        tradeHistoryRepository.save(eventMapper.toTradeHistory(matchedEvent));

        // 3. 주문 내역에서 남은 양 차감
        updateRemainingQuantity(buyOrder, sellOrder, matchedEvent.quantity());
    }

    private void updateRemainingQuantity(final Order buyOrder, final Order sellOrder, final BigDecimal quantity) {
        buyOrder.decreaseRemainingQuantity(quantity);
        sellOrder.decreaseRemainingQuantity(quantity);
    }
}