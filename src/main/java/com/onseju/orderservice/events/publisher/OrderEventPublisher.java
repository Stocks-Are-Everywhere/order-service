package com.onseju.orderservice.events.publisher;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.onseju.orderservice.events.CreatedEvent;
import com.onseju.orderservice.global.config.RabbitMQConfig;

import lombok.extern.slf4j.Slf4j;

/**
 * 주문 메세지 서비스 이벤트 발행 담당
 * 도메인 이벤트를 RabbitMQ를 통해 발행하는 기능 제공
 */
@Slf4j
@Service
public class OrderEventPublisher {

	private final RabbitTemplate rabbitTemplate;

	public OrderEventPublisher(final RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	/**
	 * 주문 생성 이벤트 발행
	 */
	public void publishOrderCreated(final CreatedEvent event) {
		rabbitTemplate.convertAndSend(
				RabbitMQConfig.ONSEJU_EXCHANGE, RabbitMQConfig.ORDER_CREATED_KEY, event);
	}
}
