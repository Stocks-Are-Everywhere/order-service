package com.onseju.orderservice.tradehistory.service;

import org.springframework.stereotype.Service;

import com.onseju.orderservice.tradehistory.domain.TradeHistory;
import com.onseju.orderservice.tradehistory.service.repository.TradeHistoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 거래 내역 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradeHistoryService {

	private final TradeHistoryRepository tradeHistoryRepository;

	/**
	 * 거래 내역 저장 (일반 사용자)
	 */
	public void saveTradeHistory(final TradeHistory tradeHistory) {
		// DB 저장
		tradeHistoryRepository.save(tradeHistory);
		log.info("거래 내역 저장");
	}
}
