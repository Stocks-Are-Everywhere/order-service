package com.onseju.orderservice.order.mapper;

import com.onseju.orderservice.order.domain.Order;
import com.onseju.orderservice.order.domain.OrderStatus;
import com.onseju.orderservice.order.service.dto.CreateOrderParams;
import com.onseju.orderservice.order.service.dto.OrderedEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

@Component
public class OrderMapper {

    // public Order toEntity(final CreateOrderParams params, final Long accountId) {
    //     return Order.builder()
    //             .companyCode(params.companyCode())
    //             .type(params.type())
    //             .totalQuantity(params.totalQuantity())
    //             .remainingQuantity(params.totalQuantity())
    //             .status(OrderStatus.ACTIVE)
    //             .price(params.price())
    //             .accountId(accountId)
    //             .timestamp()
    //             .build();
    // }

    public OrderedEvent toEvent(final Order order) {
        return new OrderedEvent(
                UUID.randomUUID(),
                order.getId(),
                order.getCompanyCode(),
                order.getType(),
                order.getStatus(),
                order.getTotalQuantity(),
                order.getRemainingQuantity(),
                order.getPrice(),
                order.getTimestamp(),
                order.getAccountId()
        );
    }

    public OrderedEvent toOrderCreateEvent(final Long id, final CreateOrderParams params, final Long accountId) {
        return new OrderedEvent(
                UUID.randomUUID(),
                id,
                params.companyCode(),
                params.type(),
                OrderStatus.ACTIVE,
                params.totalQuantity(),
                params.totalQuantity(),
                params.price(),
                Instant.now().toEpochMilli(),
                accountId
        );
    }
}
