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
        // TODO: MatchedEvent에 event id 값 추가 후 수정
        // if (event == null || event.id() == null) {
        if (event == null) {
            throw new IllegalArgumentException("Invalid order event");
        }
    }

    @Override
    protected void doPublish(MatchedEvent event) {
        try {
            publishUpdateUserEventToUserService(event);
            // log.info("주문 이벤트 발행 완료. orderId: {}", event.id());
        } catch (Exception ex) {
            // log.error("주문 이벤트 발행 중 오류 발생. orderId: {}", event.id(), ex);
            throw new MatchedEventPublisherFailException();
        }
    }


    private void publishUpdateUserEventToUserService(MatchedEvent event) {
        sendMessage(
            RabbitMQConfig.ONSEJU_EXCHANGE,
            RabbitMQConfig.USER_UPDATE_KEY,
            event,
            // "user update -" + event.id()
            "user update -" + event.buyOrderId()
        );
    }
}
