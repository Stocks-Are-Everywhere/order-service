package com.onseju.orderservice.tradehistory.repository;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.onseju.orderservice.tradehistory.domain.TradeHistory;
import com.onseju.orderservice.tradehistory.service.repository.TradeHistoryRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class TradeHistoryRepositoryImpl implements TradeHistoryRepository {

	private final TradeHistoryJpaRepository tradeHistoryJpaRepository;

	@Override
	public TradeHistory save(final TradeHistory tradeHistory) {
		return tradeHistoryJpaRepository.save(tradeHistory);
	}

	@Override
	public List<String> findDistinctCompanyCodes() {
		return tradeHistoryJpaRepository.findDistinctCompanyCodes();
	}

	@Override
	public List<TradeHistory> findRecentTradesByCompanyCode(final String companyCode, final Integer limit) {
		return tradeHistoryJpaRepository.findRecentTradesByCompanyCode(companyCode, PageRequest.of(0, limit));
	}

}
