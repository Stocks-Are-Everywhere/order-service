package com.onseju.orderservice.listener;

import com.onseju.orderservice.holding.service.HoldingsRepository;
import com.onseju.orderservice.order.domain.Order;
import com.onseju.orderservice.order.domain.OrderStatus;
import com.onseju.orderservice.order.domain.Type;
import com.onseju.orderservice.order.service.repository.OrderRepository;
import com.onseju.orderservice.tradehistory.service.TradeHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MatchedEventListenerIntegrationTest {

    @Autowired
    private MatchedEventListener matchedEventListener;
    @Autowired
    private TradeHistoryRepository tradeHistoryRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private EventMapper eventMapper;

    private MatchedEvent matchedEvent;
    @Autowired
    private HoldingsRepository holdingsRepository;

    @BeforeEach
    void setUp() throws InterruptedException {
        // 테스트를 위한 실제 데이터 생성
        Order buyOrder = Order.builder()
                .companyCode("005930")
                .type(Type.LIMIT_BUY)
                .totalQuantity(new BigDecimal("100"))
                .remainingQuantity(new BigDecimal("100"))
                .status(OrderStatus.ACTIVE)
                .price(new BigDecimal("50000"))
                .timestamp(Instant.now().getEpochSecond())
                .createdDateTime(LocalDateTime.now())
                .updatedDateTime(LocalDateTime.now())
                .accountId(1L)
                .build();
        Order sellOrder = Order.builder()
                .companyCode("005930")
                .type(Type.LIMIT_SELL)
                .totalQuantity(new BigDecimal("100"))
                .remainingQuantity(new BigDecimal("100"))
                .status(OrderStatus.ACTIVE)
                .price(new BigDecimal("50000"))
                .timestamp(Instant.now().getEpochSecond())
                .createdDateTime(LocalDateTime.now())
                .updatedDateTime(LocalDateTime.now())
                .accountId(2L)
                .build();

        // 해당 주문들을 데이터베이스에 저장
        orderRepository.save(buyOrder);
        orderRepository.save(sellOrder);

//        // 보유 주식 내역 저장
//        holdingsRepository.save(
//                Holdings.builder()
//                        .companyCode("005930")
//                        .quantity(new BigDecimal(100))
//                        .reservedQuantity(BigDecimal.ZERO)
//                        .averagePrice(new BigDecimal(1000))
//                        .totalPurchasePrice(new BigDecimal(100000))
//                        .accountId(1L)
//                        .build()
//        );
//
//        holdingsRepository.save(
//                Holdings.builder()
//                        .companyCode("005930")
//                        .quantity(new BigDecimal(100))
//                        .reservedQuantity(BigDecimal.ZERO)
//                        .averagePrice(new BigDecimal(1000))
//                        .totalPurchasePrice(new BigDecimal(100000))
//                        .accountId(2L)
//                        .build()
//        );
        Thread.sleep(1000);
        // TradeEvent 객체 준비
        matchedEvent = new MatchedEvent(
                "005930",
                1L,
                2L,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(1000),
                Instant.now().getEpochSecond()
        );
    }

    @Test
    @Transactional
    void testCreateTradeHistoryEvent() {
        // tradeEventListener 호출
        matchedEventListener.createTradeHistoryEvent(matchedEvent);

        // 2. 차감 내역 조회
        Order updatedBuyOrder = orderRepository.getById(1L);
        Order updatedSellOrder = orderRepository.getById(2L);

        assertThat(updatedBuyOrder.getRemainingQuantity()).isEqualTo(BigDecimal.ZERO);
        assertThat(updatedSellOrder.getRemainingQuantity()).isEqualTo(BigDecimal.ZERO);
    }
}