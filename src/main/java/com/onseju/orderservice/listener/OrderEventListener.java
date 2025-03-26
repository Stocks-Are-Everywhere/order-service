package com.onseju.orderservice.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.onseju.orderservice.global.config.RabbitMQConfig;
import com.onseju.orderservice.listener.dto.EventMapper;
import com.onseju.orderservice.order.service.dto.OrderedEvent;
import com.onseju.orderservice.order.service.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final OrderRepository orderRepository;
    private final EventMapper eventMapper;

    @Async
    @RabbitListener(queues = RabbitMQConfig.ORDER_QUEUE)
    public void handleOrderCreateEvent(OrderedEvent orderedEvent) {
        // 메시지를 처리하는 로직
        log.info("Received Order Create Event {}", orderedEvent);
        orderRepository.save(eventMapper.toOrder(orderedEvent));

    }
}
