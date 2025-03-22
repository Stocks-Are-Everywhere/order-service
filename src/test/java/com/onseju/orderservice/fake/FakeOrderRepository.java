package com.onseju.orderservice.fake;

import com.onseju.orderservice.order.domain.Order;
import com.onseju.orderservice.order.exception.OrderNotFoundException;
import com.onseju.orderservice.order.service.repository.OrderRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class FakeOrderRepository implements OrderRepository {

    private final List<Order> elements = new ArrayList<>();

    @Override
    public Order save(Order order) {
        if (hasElement(order)) {
            elements.remove(elements.indexOf(order));
            elements.add(order);
            return order;
        }
        Order saved = Order.builder()
                .id((long) elements.size() + 1)
                .companyCode(order.getCompanyCode())
                .type(order.getType())
                .totalQuantity(order.getTotalQuantity())
                .remainingQuantity(order.getRemainingQuantity())
                .status(order.getStatus())
                .price(order.getPrice())
                .accountId(order.getAccountId())
                .timestamp(order.getTimestamp())
                .build();
        elements.add(saved);
        return saved;
    }

    private boolean hasElement(Order order) {
        if (order.getId() == null) {
            return false;
        }
        return elements.stream()
                .anyMatch(o -> o.getId().equals(order.getId()));
    }

    @Override
    public Optional<Order> findById(Long id) {
        return elements.stream()
                .filter(e -> Objects.equals(e.getId(), id))
                .findAny();
    }

    @Override
    public Order getById(Long id) {
        return elements.stream()
                .filter(e -> Objects.equals(e.getId(), id))
                .findAny()
                .orElseThrow(OrderNotFoundException::new);
    }
}