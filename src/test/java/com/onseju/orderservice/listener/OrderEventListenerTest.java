package com.onseju.orderservice.listener;

import com.onseju.orderservice.events.OrderCreatedEvent;
import com.onseju.orderservice.events.listener.OrderEventListener;
import com.onseju.orderservice.order.domain.Order;
import com.onseju.orderservice.order.domain.OrderStatus;
import com.onseju.orderservice.order.domain.Type;
import com.onseju.orderservice.order.mapper.OrderMapper;
import com.onseju.orderservice.order.service.OrderService;
import com.onseju.orderservice.order.service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.rabbitmq.listener.simple.auto-startup=false"
})
public class OrderEventListenerTest {

	@Autowired
	private OrderEventListener orderEventListener;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	OrderService orderService;

	@Autowired
	OrderMapper orderMapper;

    private OrderCreatedEvent orderCreatedEvent;

	@BeforeEach
	void setUp() {

        orderCreatedEvent = new OrderCreatedEvent(
            UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479"),
            1L,
            "005930",
            Type.LIMIT_BUY,
            OrderStatus.ACTIVE,
            new BigDecimal("100"),
            new BigDecimal("100"),
            new BigDecimal("50000"),
            Instant.now().toEpochMilli(),
            1L
        );
	}

	@Test
    @Transactional
	void testOrderCreatedEvent() {
        // Given
		orderEventListener.handleOrderCreated(orderCreatedEvent);

        // When
        Order order = orderRepository.findById(orderCreatedEvent.orderId()).orElseThrow();

        // Then
        assertThat(order.getId()).isEqualTo(orderCreatedEvent.orderId());
		assertThat(order.getStatus()).isEqualTo(OrderStatus.ACTIVE);
        assertThat(order.getRemainingQuantity()).isEqualTo(orderCreatedEvent.totalQuantity());
		assertThat(order.getPrice()).isEqualTo(orderCreatedEvent.price());
		assertThat(order.getCompanyCode()).isEqualTo(orderCreatedEvent.companyCode());
		assertThat(order.getType()).isEqualTo(orderCreatedEvent.type());
		assertThat(order.getTimestamp()).isEqualTo(orderCreatedEvent.timestamp());
	}
}