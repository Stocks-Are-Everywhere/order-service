package com.onseju.orderservice.tradehistory.service.repository;

import com.onseju.orderservice.tradehistory.domain.TradeHistory;
import org.springframework.data.domain.Pageable;

import java.util.List;


public interface TradeHistoryRepository  {

	TradeHistory save(final TradeHistory tradeHistory);

	List<String> findDistinctCompanyCodes();

	List<TradeHistory> findRecentTradesByCompanyCode(final String companyCode, final Integer limit);

	List<Object[]> findTotalTradeAmountByCompany(Pageable pageable);

	List<Object[]> findTradeAvgPriceByCompany(Pageable pageable);

	List<Object[]> findTradeCountByCompany(Pageable pageable);

}
