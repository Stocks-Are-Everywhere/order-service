package com.onseju.orderservice.tradehistory.repository;

import com.onseju.orderservice.tradehistory.domain.TradeHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TradeHistoryJpaRepository extends JpaRepository<TradeHistory, Long> {

	/**
	 * 모든 고유 회사 코드 조회
	 */
	@Query("SELECT DISTINCT t.companyCode FROM TradeHistory t")
	List<String> findDistinctCompanyCodes();

	/**
	 * 특정 회사의 최근 거래 내역 조회
	 */
	@Query("SELECT t FROM TradeHistory t WHERE t.companyCode = :companyCode ORDER BY t.tradeTime DESC")
	List<TradeHistory> findRecentTradesByCompanyCode(@Param("companyCode") String companyCode, Pageable pageable);

	/**
	 * 회사별 총 거래액 조회
	 */
	@Query("SELECT t.companyCode, SUM(t.price * t.quantity) as totalAmount " +
		"FROM TradeHistory t GROUP BY t.companyCode ORDER BY totalAmount DESC")
	List<Object[]> findTotalTradeAmountByCompany(Pageable pageable);

	/**
	 * 회사별 평균 가격 조회
	 */
	@Query("SELECT t.companyCode, AVG(t.price) as avgPrice " +
		"FROM TradeHistory t GROUP BY t.companyCode ORDER BY avgPrice DESC")
	List<Object[]> findTradeAvgPriceByCompany(Pageable pageable);

	/**
	 * 회사별 거래 건수 조회
	 */
	@Query("SELECT t.companyCode, COUNT(t) as tradeCount " +
		"FROM TradeHistory t GROUP BY t.companyCode ORDER BY tradeCount DESC")
	List<Object[]> findTradeCountByCompany(Pageable pageable);

	List<TradeHistory> findBySellOrderId(Long orderId);

	List<TradeHistory> findByBuyOrderId(Long orderId);
}
