package com.onseju.orderservice.producer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.onseju.orderservice.producer.exception.EventProduceFailException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractEventProducer<T> implements EventProducer<T> {
    private static final int OPERATION_TIMEOUT_SECONDS = 10;

    protected final RabbitTemplate rabbitTemplate;

    public CompletableFuture<Void> publishEvent(T event) {
        return CompletableFuture.runAsync(() -> {
            validateEvent(event);
            doPublish(event);
        }).orTimeout(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    protected abstract void validateEvent(T event);
    protected abstract void doPublish(T event);

    protected void sendMessage(String exchange, String routingKey,
            T event, String correlationId) {
        try {
            CorrelationData correlation = new CorrelationData(correlationId);
            rabbitTemplate.convertAndSend(exchange, routingKey, event, correlation);

            CorrelationData.Confirm confirm = correlation.getFuture().get();

            if (confirm == null || !confirm.isAck()) {
                throw new EventProduceFailException();
            }
        } catch (Exception e) {
            log.error("메시지 발행 오류 발생: {}, correlationId: {}", e.getMessage(), correlationId);
            throw new EventProduceFailException();
        }
    }
}
