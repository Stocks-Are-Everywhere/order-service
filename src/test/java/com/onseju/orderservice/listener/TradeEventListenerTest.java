package com.onseju.orderservice.listener;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.onseju.orderservice.chart.service.ChartService;
import com.onseju.orderservice.events.MatchedEvent;
import com.onseju.orderservice.events.listener.OrderEventListener;
import com.onseju.orderservice.order.domain.Order;
import com.onseju.orderservice.order.domain.OrderStatus;
import com.onseju.orderservice.order.domain.Type;
import com.onseju.orderservice.order.mapper.OrderMapper;
import com.onseju.orderservice.order.service.OrderService;
import com.onseju.orderservice.order.service.repository.OrderRepository;
import com.onseju.orderservice.tradehistory.mapper.TradeHistoryMapper;
import com.onseju.orderservice.tradehistory.repository.TradeHistoryJpaRepository;
import com.onseju.orderservice.tradehistory.service.TradeHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest()
class TradeEventListenerTest {

	@Autowired
	private OrderEventListener orderEventListener;

	@Autowired
	TradeHistoryService tradeHistoryService;

	@Autowired
	TradeHistoryMapper tradeHistoryMapper;

	@Autowired
	OrderService orderService;

	@Autowired
	OrderMapper orderMapper;

	@Autowired
	ChartService chartService;

	@Autowired
	private TradeHistoryJpaRepository tradeHistoryJpaRepository;

	@Autowired
	private OrderRepository orderRepository;

	private MatchedEvent matchedEvent;

	@BeforeEach
	void setUp() {
		// 테스트를 위한 실제 데이터 생성
		Order buyOrder = Order.builder()
				.companyCode("005930")
				.type(Type.LIMIT_BUY)
				.totalQuantity(new BigDecimal("100"))
				.remainingQuantity(new BigDecimal("100"))
				.status(OrderStatus.ACTIVE)
				.price(new BigDecimal("50000"))
				.timestamp(Instant.now().toEpochMilli())
				.createdDateTime(LocalDateTime.now())
				.updatedDateTime(LocalDateTime.now())
				.build();
		Order sellOrder = Order.builder()
				.companyCode("005930")
				.type(Type.LIMIT_SELL)
				.totalQuantity(new BigDecimal("100"))
				.remainingQuantity(new BigDecimal("100"))
				.status(OrderStatus.ACTIVE)
				.price(new BigDecimal("50000"))
				.timestamp(Instant.now().toEpochMilli())
				.createdDateTime(LocalDateTime.now())
				.updatedDateTime(LocalDateTime.now())
				.build();

		// 해당 주문들을 데이터베이스에 저장
		orderRepository.save(buyOrder);
		orderRepository.save(sellOrder);

		matchedEvent = new MatchedEvent(
				UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479"),
				"005930",
				1L,
				1L,
				2L,
				2L,
				BigDecimal.valueOf(100),
				BigDecimal.valueOf(1000),
				Instant.now().toEpochMilli()
		);
	}

	@Test
	@Transactional
	void testCreateTradeHistoryEvent() {
		// tradeEventListener 호출
		orderEventListener.handleOrderMatched(matchedEvent);

		// 2. 차감 내역 조회
		Order updatedBuyOrder = orderRepository.getById(1L);
		Order updatedSellOrder = orderRepository.getById(2L);

		assertThat(updatedBuyOrder.getRemainingQuantity()).isEqualTo(BigDecimal.ZERO);
		assertThat(updatedSellOrder.getRemainingQuantity()).isEqualTo(BigDecimal.ZERO);
	}
}
