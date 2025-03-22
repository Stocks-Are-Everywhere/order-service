package com.onseju.orderservice.order.repository;

import com.onseju.orderservice.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {

}
