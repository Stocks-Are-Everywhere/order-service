package com.onseju.orderservice.events.listener;

import com.onseju.orderservice.events.OrderCreatedEvent;
import com.onseju.orderservice.global.config.RabbitMQConfig;
import com.onseju.orderservice.order.domain.Order;
import com.onseju.orderservice.order.mapper.OrderMapper;
import com.onseju.orderservice.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 주문 서비스의 체결 이벤트 리스너
 * RabbitMQ를 통해 수신된 이벤트를 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

	private final OrderService orderService;
	private final OrderMapper orderMapper;

	@RabbitListener(queues = RabbitMQConfig.ORDER_CREATED_QUEUE)
	public void handleOrderCreated(final OrderCreatedEvent event) {
		log.info("OrderCreatedEvent received: {}", event);
		Order order = orderMapper.toEntity(event);
		orderService.saveOrder(order);
	}
}
