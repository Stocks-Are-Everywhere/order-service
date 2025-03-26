package com.onseju.orderservice.order.mapper;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.stereotype.Component;

import com.onseju.orderservice.events.CreatedEvent;
import com.onseju.orderservice.order.domain.Order;
import com.onseju.orderservice.order.domain.OrderStatus;
import com.onseju.orderservice.order.dto.AfterTradeOrderDto;
import com.onseju.orderservice.order.dto.BeforeTradeOrderDto;

@Component
public class OrderMapper {

	public Order toEntity(final BeforeTradeOrderDto dto, final Long accountId) {
		return Order.builder()
				.companyCode(dto.companyCode())
				.type(dto.type())
				.totalQuantity(dto.totalQuantity())
				.remainingQuantity(dto.totalQuantity())
				.status(OrderStatus.ACTIVE)
				.price(dto.price())
				.accountId(accountId)
				.timestamp(Instant.now().getEpochSecond())
				.build();
	}

	public CreatedEvent toEvent(final Order order) {
		return CreatedEvent.builder()
				.id(order.getId())
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
