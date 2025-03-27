package com.onseju.orderservice.events.publisher;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.onseju.orderservice.events.MatchedEvent;
import com.onseju.orderservice.events.exception.MatchedEventPublisherFailException;
import com.onseju.orderservice.global.config.RabbitMQConfig;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MatchedEventPublisher extends AbstractEventPublisher<MatchedEvent> {

    public MatchedEventPublisher(RabbitTemplate rabbitTemplate) {
        super(rabbitTemplate);
    }

    @Override
    protected void validateEvent(MatchedEvent event) {
        if (event == null || event.id() == null) {
            throw new IllegalArgumentException("Invalid matched order event");
        }
    }

    @Override
    protected void doPublish(MatchedEvent event) {
        try {
            publishUpdateUserEventToUserService(event);
        } catch (Exception ex) {
            log.error("체결 완료 이벤트 발행 중 오류 발생. event id: {}", event.id(), ex);
            throw new MatchedEventPublisherFailException();
        }
    }


    private void publishUpdateUserEventToUserService(MatchedEvent event) {
        sendMessage(
            RabbitMQConfig.ONSEJU_EXCHANGE,
            RabbitMQConfig.USER_UPDATE_KEY,
            event,
            "user update -" + event.id()
        );
    }
}
