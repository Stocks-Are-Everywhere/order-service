package com.onseju.orderservice.order.repository;

import com.onseju.orderservice.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {
    List<Order> findByAccountId(Long accountId);
}
