package com.onseju.orderservice.order.repository;


import com.onseju.orderservice.order.domain.Order;
import com.onseju.orderservice.order.exception.OrderNotFoundException;
import com.onseju.orderservice.order.service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

	private final OrderJpaRepository orderJpaRepository;

	@Override
	public Order save(final Order order) {
		return orderJpaRepository.save(order);
	}

	@Override
	public Optional<Order> findById(final Long id) {
		return orderJpaRepository.findById(id);
	}

	@Override
	public Order getById(Long id) {
		return orderJpaRepository
				.findById(id)
				.orElseThrow(OrderNotFoundException::new);
	}
}
