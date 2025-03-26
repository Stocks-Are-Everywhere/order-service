package com.onseju.orderservice.listener;

import com.onseju.orderservice.global.config.RabbitMQConfig;
import com.onseju.orderservice.listener.dto.EventMapper;
import com.onseju.orderservice.listener.dto.MatchedEvent;
import com.onseju.orderservice.order.domain.Order;
import com.onseju.orderservice.order.service.repository.OrderRepository;
import com.onseju.orderservice.tradehistory.service.TradeHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchedEventListener {

    private final TradeHistoryRepository tradeHistoryRepository;
    private final OrderRepository orderRepository;
    private final EventMapper eventMapper;

    @Async
    @RabbitListener(queues = RabbitMQConfig.MATCHING_RESULT_QUEUE)
    public void createTradeHistoryEvent(final MatchedEvent matchedEvent) {
        log.info("Received Matched Event {}", matchedEvent);
        // 1. 주문 내역 조회
        Order buyOrder = orderRepository.getById(matchedEvent.buyOrderId());
        Order sellOrder = orderRepository.getById(matchedEvent.sellOrderId());

        // 2. 체결 내역 저장
        tradeHistoryRepository.save(eventMapper.toTradeHistory(matchedEvent));

        // 3. 주문 내역에서 남은 양 차감
        updateRemainingQuantity(buyOrder, sellOrder, matchedEvent.quantity());

        // 4. 주문 내역 업데이트
        orderRepository.save(buyOrder);
        orderRepository.save(sellOrder);

        // TODO: 5. 체결 내역 이벤트 발행 OR Websocket
        // 잔액 변경
        // 보유 주식 변경
    }

    private void updateRemainingQuantity(final Order buyOrder, final Order sellOrder, final BigDecimal quantity) {
        buyOrder.decreaseRemainingQuantity(quantity);
        sellOrder.decreaseRemainingQuantity(quantity);
    }
}