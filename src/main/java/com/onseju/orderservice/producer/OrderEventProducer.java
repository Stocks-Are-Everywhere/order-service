package com.onseju.orderservice.producer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.onseju.orderservice.global.config.RabbitMQConfig;
import com.onseju.orderservice.order.service.dto.OrderedEvent;
import com.onseju.orderservice.producer.exception.OrderEventProduceFailException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OrderEventProducer extends AbstractEventProducer<OrderedEvent> {

    public OrderEventProducer(RabbitTemplate rabbitTemplate) {
        super(rabbitTemplate);
    }

    @Override
    protected void validateEvent(OrderedEvent event) {
        if (event == null || event.id() == null) {
            throw new IllegalArgumentException("Invalid order event");
        }
    }

    @Override
    protected void doPublish(OrderedEvent event) {
        try {
            publishOrderEvent(event);
            publishMatchingEvent(event);
            log.info("주문 이벤트 발행 완료. orderId: {}", event.id());
        } catch (Exception ex) {
            log.error("주문 이벤트 발행 중 오류 발생. orderId: {}", event.id(), ex);
            throw new OrderEventProduceFailException();
        }
    }

    private void publishOrderEvent(OrderedEvent event) {
        sendMessage(
            RabbitMQConfig.ORDER_EXCHANGE,
            RabbitMQConfig.ORDER_CREATE_KEY,
            event,
            "order-" + event.id()
        );
    }

    private void publishMatchingEvent(OrderedEvent event) {
        sendMessage(
            RabbitMQConfig.MATCHING_EXCHANGE,
            RabbitMQConfig.MATCHING_REQUEST_KEY,
            event,
            "matching-" + event.id()
        );
    }
}