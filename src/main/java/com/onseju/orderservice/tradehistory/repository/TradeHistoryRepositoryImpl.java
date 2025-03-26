package com.onseju.orderservice.tradehistory.repository;

import com.onseju.orderservice.tradehistory.domain.TradeHistory;
import com.onseju.orderservice.tradehistory.service.TradeHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TradeHistoryRepositoryImpl implements TradeHistoryRepository {

	private final TradeHistoryJpaRepository tradeHistoryJpaRepository;

	@Override
	public TradeHistory save(final TradeHistory tradeHistory) {
		return tradeHistoryJpaRepository.save(tradeHistory);
	}

	@Override
	public void deleteAll() {
		tradeHistoryJpaRepository.deleteAll();
	}

	@Override
	public TradeHistory findByBuyOrderIdAndSellOrderId(Long buyOrderId, Long sellOrderId) {
		return tradeHistoryJpaRepository.findByBuyOrderIdAndSellOrderId(buyOrderId, sellOrderId);
	}
}
