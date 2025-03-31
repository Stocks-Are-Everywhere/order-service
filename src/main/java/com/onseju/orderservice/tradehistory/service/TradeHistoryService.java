package com.onseju.orderservice.tradehistory.service;

import com.onseju.orderservice.order.domain.Order;
import com.onseju.orderservice.order.service.repository.OrderRepository;
import com.onseju.orderservice.tradehistory.domain.TradeHistory;
import com.onseju.orderservice.tradehistory.dto.TradeHistoryResponse;
import com.onseju.orderservice.tradehistory.mapper.TradeHistoryMapper;
import com.onseju.orderservice.tradehistory.service.repository.TradeHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 거래 내역 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradeHistoryService {

	private final TradeHistoryRepository tradeHistoryRepository;
	private final OrderRepository orderRepository;
	private final TradeHistoryMapper tradeHistoryMapper;

	/**
	 * 거래 내역 저장 (일반 사용자)
	 */
	public void saveTradeHistory(final TradeHistory tradeHistory) {
		// DB 저장
		tradeHistoryRepository.save(tradeHistory);
		log.info("거래 내역 저장");
	}

	public Collection<TradeHistoryResponse> getAllTradeHistory(Long memberId) {
		List<Order> orders = orderRepository.findByMemberId(memberId);
		return orders.stream()
				.map(order ->
						tradeHistoryRepository.findByOrderId(order).stream()
								.map(tradeHistory -> tradeHistoryMapper.toResponse(tradeHistory, order))
								.toList()
				)
				.flatMap(Collection::stream)
				.filter(Objects::nonNull)
				.toList();
	}
}
