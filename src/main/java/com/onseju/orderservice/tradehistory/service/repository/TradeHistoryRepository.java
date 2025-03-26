package com.onseju.orderservice.tradehistory.service.repository;

import java.util.List;

import com.onseju.orderservice.tradehistory.domain.TradeHistory;

public interface TradeHistoryRepository {

	TradeHistory save(final TradeHistory tradeHistory);

	/**
	 * 모든 고유 회사 코드 조회
	 */
	List<String> findDistinctCompanyCodes();

	/**
	 * 특정 회사의 최근 N개 거래 내역 조회
	 */
	List<TradeHistory> findRecentTradesByCompanyCode(final String companyCode, final Integer limit);

}
