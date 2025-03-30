package com.onseju.orderservice.order.mapper;

import com.onseju.orderservice.events.OrderCreatedEvent;
import com.onseju.orderservice.order.domain.Order;
import com.onseju.orderservice.order.domain.OrderStatus;
import com.onseju.orderservice.order.domain.Type;
import com.onseju.orderservice.order.dto.AfterTradeOrderDto;
import com.onseju.orderservice.order.dto.BeforeTradeOrderDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMapperTest {

	private final OrderMapper orderMapper = new OrderMapper();

	@Test
	@DisplayName("OrderRequest를 Entity로 변환한다.")
	void toEntity() {
		// given
		String companyCode = "005930";
		BeforeTradeOrderDto beforeTradeOrderDto = new BeforeTradeOrderDto("005930", "LIMIT_BUY", new BigDecimal(100),
			new BigDecimal(1000), 1L);

		// when
		Order order = orderMapper.toEntity(1L, beforeTradeOrderDto, 1L);

		// then
		assertThat(order).isNotNull();
		assertThat(order.getCompanyCode()).isEqualTo(companyCode);
	}

	@Test
	@DisplayName("OrderCreatedEvent를 Entity로 변환한다.")
	void toEntityFromEvent() {
		// given
		OrderCreatedEvent orderCreatedEvent = OrderCreatedEvent.builder()
			.id(UUID.randomUUID())
			.orderId(1L)
			.companyCode("005930")
			.type(Type.LIMIT_BUY)
			.status(OrderStatus.ACTIVE)
			.totalQuantity(new BigDecimal(100))
			.remainingQuantity(new BigDecimal(100))
			.price(new BigDecimal(50000))
			.timestamp(Instant.now().toEpochMilli())
			.accountId(1L)
			.build();

		// when
		Order order = orderMapper.toEntity(orderCreatedEvent);

		// then
		assertThat(order).isNotNull();
		assertThat(order.getId()).isEqualTo(orderCreatedEvent.orderId());
		assertThat(order.getCompanyCode()).isEqualTo(orderCreatedEvent.companyCode());
		assertThat(order.getType()).isEqualTo(orderCreatedEvent.type());
		assertThat(order.getStatus()).isEqualTo(orderCreatedEvent.status());
		assertThat(order.getTotalQuantity()).isEqualTo(orderCreatedEvent.totalQuantity());
		assertThat(order.getRemainingQuantity()).isEqualTo(orderCreatedEvent.remainingQuantity());
		assertThat(order.getPrice()).isEqualTo(orderCreatedEvent.price());
		assertThat(order.getTimestamp()).isEqualTo(orderCreatedEvent.timestamp());
	}

	@Test
	@DisplayName("Order Entity를 OrderCreatedEvent로 변환한다.")
	void toEvent() {
		// given
		Order order = Order.builder()
			.id(1L)
			.companyCode("005930")
			.type(Type.LIMIT_BUY)
			.status(OrderStatus.ACTIVE)
			.totalQuantity(new BigDecimal(100))
			.remainingQuantity(new BigDecimal(100))
			.price(new BigDecimal(50000))
			.timestamp(Instant.now().toEpochMilli())
			.accountId(1L)
			.build();

		// when
		OrderCreatedEvent orderCreatedEvent = orderMapper.toEvent(order);

		// then
		assertThat(orderCreatedEvent).isNotNull();
		assertThat(orderCreatedEvent.orderId()).isEqualTo(order.getId());
		assertThat(orderCreatedEvent.companyCode()).isEqualTo(order.getCompanyCode());
		assertThat(orderCreatedEvent.type()).isEqualTo(order.getType());
		assertThat(orderCreatedEvent.status()).isEqualTo(order.getStatus());
		assertThat(orderCreatedEvent.totalQuantity()).isEqualTo(order.getTotalQuantity());
		assertThat(orderCreatedEvent.remainingQuantity()).isEqualTo(order.getRemainingQuantity());
	}

	@Test
	@DisplayName("Order service 에서 사용하는 AfterTradeOrderDto로 변환한다.")
	void toAfterTradeOrderDto() {
		// given
		Long orderId = 1L;
		BigDecimal quantity = new BigDecimal(100);

		// when
		AfterTradeOrderDto afterTradeOrderDto = orderMapper.toAfterTradeOrderDto(orderId, quantity);

		// then
		assertThat(afterTradeOrderDto).isNotNull();
		assertThat(afterTradeOrderDto.orderId()).isEqualTo(orderId);
		assertThat(afterTradeOrderDto.quantity()).isEqualTo(quantity);
	}
}
