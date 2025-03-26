package com.onseju.orderservice.listener;

import com.onseju.orderservice.global.config.ServiceProperties;
import com.onseju.orderservice.holding.service.HoldingsRepository;
import com.onseju.orderservice.order.domain.Order;
import com.onseju.orderservice.order.domain.OrderStatus;
import com.onseju.orderservice.order.domain.Type;
import com.onseju.orderservice.order.service.repository.OrderRepository;
import com.onseju.orderservice.tradehistory.domain.TradeHistory;
import com.onseju.orderservice.tradehistory.service.TradeHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@TestPropertySource(properties = {
	"services.user-service.url=http://localhost:8080"
})
class MatchedEventListenerIntegrationTest {

	@Autowired
	private MatchedEventListener matchedEventListener;

	@Autowired
	private TradeHistoryRepository tradeHistoryRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private EventMapper eventMapper;

	@MockBean
	private HoldingsRepository holdingsRepository;

	@MockBean
	private ServiceProperties properties;

	private Order savedBuyOrder;
	private Order savedSellOrder;
	private MatchedEvent matchedEvent;

	@BeforeEach
	void setUp() {
		// 기존 데이터 정리
		tradeHistoryRepository.deleteAll();
		orderRepository.deleteAll();

		// 테스트를 위한 실제 데이터 생성
		Order buyOrder = createOrder(Type.LIMIT_BUY, 1L);
		Order sellOrder = createOrder(Type.LIMIT_SELL, 2L);

		// 해당 주문들을 데이터베이스에 저장하고 저장된 엔티티 참조
		savedBuyOrder = orderRepository.save(buyOrder);
		savedSellOrder = orderRepository.save(sellOrder);

		// MatchedEvent 객체 준비 - 저장된 주문의 실제 ID 사용
		matchedEvent = new MatchedEvent(
			"005930",
			savedBuyOrder.getId(),
			savedSellOrder.getId(),
			new BigDecimal("50"), // 부분 체결을 테스트하기 위해 50으로 설정
			new BigDecimal("50000"),
			Instant.now().getEpochSecond()
		);
	}

	private Order createOrder(Type type, Long accountId) {
		return Order.builder()
			.companyCode("005930")
			.type(type)
			.totalQuantity(new BigDecimal("100"))
			.remainingQuantity(new BigDecimal("100"))
			.status(OrderStatus.ACTIVE)
			.price(new BigDecimal("50000"))
			.timestamp(Instant.now().getEpochSecond())
			.createdDateTime(LocalDateTime.now())
			.updatedDateTime(LocalDateTime.now())
			.accountId(accountId)
			.build();
	}

	@Test
	void testCreateTradeHistoryEvent() {
		// Given: setUp에서 준비된 데이터

		// When: matchedEventListener 호출
		matchedEventListener.createTradeHistoryEvent(matchedEvent);

		// Then: 결과 검증

		// 1. 체결 내역이 저장되었는지 확인
		TradeHistory savedTradeHistory = tradeHistoryRepository.findByBuyOrderIdAndSellOrderId(
			savedBuyOrder.getId(), savedSellOrder.getId());

		assertThat(savedTradeHistory).isNotNull();
		assertThat(savedTradeHistory.getQuantity()).isEqualByComparingTo(new BigDecimal("50"));
		assertThat(savedTradeHistory.getPrice()).isEqualByComparingTo(new BigDecimal("50000"));

		// 2. 주문의 잔여 수량이 정확히 차감되었는지 확인
		Order updatedBuyOrder = orderRepository.findById(savedBuyOrder.getId()).orElseThrow();
		Order updatedSellOrder = orderRepository.findById(savedSellOrder.getId()).orElseThrow();

		assertThat(updatedBuyOrder.getRemainingQuantity()).isEqualByComparingTo(new BigDecimal("50"));
		assertThat(updatedSellOrder.getRemainingQuantity()).isEqualByComparingTo(new BigDecimal("50"));

		// 3. 주문 상태가 적절히 변경되었는지 확인 (부분 체결이므로 여전히 ACTIVE 상태여야 함)
		assertThat(updatedBuyOrder.getStatus()).isEqualTo(OrderStatus.ACTIVE);
		assertThat(updatedSellOrder.getStatus()).isEqualTo(OrderStatus.ACTIVE);

		// 4. 완전 체결 테스트 - 나머지 수량에 대한 체결 이벤트 생성
		MatchedEvent completeMatchEvent = new MatchedEvent(
			"005930",
			savedBuyOrder.getId(),
			savedSellOrder.getId(),
			new BigDecimal("50"), // 남은 수량 전체 체결
			new BigDecimal("50000"),
			Instant.now().getEpochSecond()
		);

		matchedEventListener.createTradeHistoryEvent(completeMatchEvent);

		// 완전 체결 후 상태 확인
		updatedBuyOrder = orderRepository.findById(savedBuyOrder.getId()).orElseThrow();
		updatedSellOrder = orderRepository.findById(savedSellOrder.getId()).orElseThrow();

		assertThat(updatedBuyOrder.getRemainingQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(updatedSellOrder.getRemainingQuantity()).isEqualByComparingTo(BigDecimal.ZERO);

		// 주문이 완전히 체결되었으므로 상태가 COMPLETED로 변경되었는지 확인
		assertThat(updatedBuyOrder.getStatus()).isEqualTo(OrderStatus.COMPLETE);
		assertThat(updatedSellOrder.getStatus()).isEqualTo(OrderStatus.COMPLETE);
	}
}

