package com.onseju.orderservice.tradehistory.service;

import com.onseju.orderservice.tradehistory.dto.TradeHistoryDto;
import com.onseju.orderservice.tradehistory.mapper.TradeHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 거래 내역 관리 서비스
 */
@Service
@RequiredArgsConstructor
public class TradeHistoryService {

	private final TradeHistoryRepository tradeHistoryRepository;
	private final TradeHistoryMapper tradeHistoryMapper;

	/**
	 * 거래 내역 저장 (일반 사용자)
	 */
	@Transactional
	public void save(final TradeHistoryDto tradeHistoryDto) {
		tradeHistoryRepository.save(tradeHistoryMapper.toEntity(tradeHistoryDto));
	}
}
