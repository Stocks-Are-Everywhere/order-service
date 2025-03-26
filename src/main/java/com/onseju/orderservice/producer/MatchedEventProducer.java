package com.onseju.orderservice.producer;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.onseju.orderservice.global.config.RabbitMQConfig;
import com.onseju.orderservice.order.service.dto.MatchedEvent;
import com.onseju.orderservice.order.service.dto.OrderedEvent;
import com.onseju.orderservice.producer.exception.OrderEventProduceFailException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MatchedEventProducer extends AbstractEventProducer<MatchedEvent> {

    public MatchedEventProducer(RabbitTemplate rabbitTemplate) {
        super(rabbitTemplate);
    }

    @Override
    protected void validateEvent(MatchedEvent event) {
        if (event == null || event.id() == null) {
            throw new IllegalArgumentException("Invalid order event");
        }
    }

    @Override
    protected void doPublish(MatchedEvent event) {
        try {
            publishUpdateUserEvent(event);
            log.info("주문 이벤트 발행 완료. orderId: {}", event.id());
        } catch (Exception ex) {
            log.error("주문 이벤트 발행 중 오류 발생. orderId: {}", event.id(), ex);
            throw new OrderEventProduceFailException();
        }
    }


    private void publishUpdateUserEvent(MatchedEvent event) {
        sendMessage(
            RabbitMQConfig.USER_EXCHANGE,
            RabbitMQConfig.USER_UPDATE_KEY,
            event,
            "user update -" + event.id()
        );
    }
}
