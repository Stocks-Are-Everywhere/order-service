package com.onseju.orderservice.listener;

import com.onseju.orderservice.order.service.OrderService;
import com.onseju.orderservice.tradehistory.service.TradeHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class MatchedEventListener {

    private final TradeHistoryService tradeHistoryService;
    private final OrderService orderService;
    private final EventMapper eventMapper;

    @Async
    @Transactional
    @RabbitListener(queues = "matched.order.queue")
    public void createTradeHistoryEvent(final MatchedEvent matchedEvent) {
        log.info("Matched event: {}", matchedEvent);

        // 1. 체결 내역 저장
        tradeHistoryService.save(eventMapper.toTradeHistoryDto(matchedEvent));

        // 2. 주문 내역에서 남은 양 차감
        orderService.updateRemainingQuantity(eventMapper.toMatchedOrderDto(matchedEvent));
    }
}