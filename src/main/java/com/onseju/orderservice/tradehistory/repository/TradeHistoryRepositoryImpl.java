package com.onseju.orderservice.tradehistory.repository;

import com.onseju.orderservice.order.domain.Order;
import com.onseju.orderservice.tradehistory.domain.TradeHistory;
import com.onseju.orderservice.tradehistory.service.repository.TradeHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

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

	/**
	 * 회사별 총 거래액 조회
	 */
	@Override
	public List<Object[]> findTotalTradeAmountByCompany(Pageable pageable) {
		return tradeHistoryJpaRepository.findTotalTradeAmountByCompany(pageable);
	}

	/**
	 * 회사별 평균 가격 조회
	 */
	@Override
	public List<Object[]> findTradeAvgPriceByCompany(Pageable pageable) {
		return tradeHistoryJpaRepository.findTradeAvgPriceByCompany(pageable);
	}

	/**
	 * 회사별 거래 건수 조회
	 */
	@Override
	public List<Object[]> findTradeCountByCompany(Pageable pageable) {
		return tradeHistoryJpaRepository.findTradeCountByCompany(pageable);
	}

	@Override
	public List<TradeHistory> findByOrderId(Order order) {
		if (order.getType().isBuy()) {
			return tradeHistoryJpaRepository.findByBuyOrderId(order.getId());
		}
		return tradeHistoryJpaRepository.findBySellOrderId(order.getId());
	}
}
