package com.onseju.orderservice.order.mapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.onseju.orderservice.events.OrderCreatedEvent;
import com.onseju.orderservice.order.domain.Order;
import com.onseju.orderservice.order.domain.OrderStatus;
import com.onseju.orderservice.order.domain.Type;
import com.onseju.orderservice.order.dto.AfterTradeOrderDto;
import com.onseju.orderservice.order.dto.BeforeTradeOrderDto;

@Component
public class OrderMapper {
	public Order toEntity(final Long orderId, final BeforeTradeOrderDto dto, final Long accountId) {
		return Order.builder()
				.id(orderId)
				.companyCode(dto.companyCode())
				.type(Type.valueOf(dto.type()))
				.totalQuantity(dto.totalQuantity())
				.remainingQuantity(dto.totalQuantity())
				.status(OrderStatus.ACTIVE)
				.price(dto.price())
				.accountId(accountId)
				.timestamp(Instant.now().toEpochMilli())
				.build();
	}

	public Order toEntity(final OrderCreatedEvent event) {
		return Order.builder()
				.id(event.orderId())
				.companyCode(event.companyCode())
				.type(event.type())
				.totalQuantity(event.totalQuantity())
				.remainingQuantity(event.totalQuantity())
				.status(OrderStatus.ACTIVE)
				.price(event.price())
				.accountId(event.accountId())
				.timestamp(event.timestamp())
				.build();
	}

	public OrderCreatedEvent toEvent(final Order order) {
		return OrderCreatedEvent.builder()
				.id(UUID.randomUUID())
				.orderId(order.getId())
				.companyCode(order.getCompanyCode())
				.type(order.getType())
				.status(order.getStatus())
				.totalQuantity(order.getTotalQuantity())
				.remainingQuantity(order.getRemainingQuantity())
				.price(order.getPrice())
				.timestamp(order.getTimestamp())
				.accountId(order.getAccountId())
				.build();
	}

	public AfterTradeOrderDto toAfterTradeOrderDto(final Long orderId, final BigDecimal quantity) {
		return AfterTradeOrderDto.builder()
				.orderId(orderId)
				.quantity(quantity)
				.build();
	}
}
