package com.onseju.orderservice.events.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.onseju.orderservice.chart.service.ChartService;
import com.onseju.orderservice.events.MatchedEvent;
import com.onseju.orderservice.events.OrderBookSyncedEvent;
import com.onseju.orderservice.global.config.RabbitMQConfig;
import com.onseju.orderservice.order.dto.AfterTradeOrderDto;
import com.onseju.orderservice.order.mapper.OrderMapper;
import com.onseju.orderservice.order.service.OrderService;
import com.onseju.orderservice.tradehistory.domain.TradeHistory;
import com.onseju.orderservice.tradehistory.mapper.TradeHistoryMapper;
import com.onseju.orderservice.tradehistory.service.TradeHistoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 주문 서비스의 체결 이벤트 리스너
 * RabbitMQ를 통해 수신된 이벤트를 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchedEventListener {

	private final TradeHistoryService tradeHistoryService;
	private final TradeHistoryMapper tradeHistoryMapper;

	private final OrderService orderService;
	private final OrderMapper orderMapper;

	private final ChartService chartService;

	/**
	 * 주문 매칭 이벤트 처리
	 * 매칭 엔진에서 매칭이 발생하면 거래 내역 생성
	 */
	@RabbitListener(queues = RabbitMQConfig.MATCHING_RESULT_QUEUE)
	public void handleOrderMatched(final MatchedEvent event) {
		// 체결 내역 저장
		final TradeHistory tradeHistory = tradeHistoryMapper.toEntity(event);
		tradeHistoryService.saveTradeHistory(tradeHistory);

		// 메모리에 거래 내역 저장 및 차트 업데이트
		chartService.processNewTrade(tradeHistory);

		// 주문 내역에서 남은 양 차감
		final AfterTradeOrderDto buyOrder =
				orderMapper.toAfterTradeOrderDto(event.buyOrderId(), event.quantity());
		final AfterTradeOrderDto sellOrder =
				orderMapper.toAfterTradeOrderDto(event.sellOrderId(), event.quantity());

		orderService.updateRemainingQuantity(buyOrder);
		orderService.updateRemainingQuantity(sellOrder);

		// 매칭 후, 사용자 업데이트 이벤트 발행
		orderService.publishUserUpdateEvent(event);
	}

	/**
	 * 호가창 이벤트 처리
	 */
	@RabbitListener(queues = RabbitMQConfig.ORDER_BOOK_SYNCED_QUEUE)
	public void handleOrderBookSynced(final OrderBookSyncedEvent event) {
		orderService.broadcastOrderBookUpdate(event);
	}
}
