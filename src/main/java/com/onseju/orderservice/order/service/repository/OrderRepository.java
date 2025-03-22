package com.onseju.orderservice.order.service.repository;

import com.onseju.orderservice.order.domain.Order;

import java.util.Optional;

public interface OrderRepository {

	Order save(final Order order);

	Optional<Order> findById(final Long id);

	Order getById(final Long id);
}
