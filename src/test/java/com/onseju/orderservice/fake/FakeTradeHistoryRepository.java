package com.onseju.orderservice.fake;

import com.onseju.orderservice.tradehistory.domain.TradeHistory;
import com.onseju.orderservice.tradehistory.service.repository.TradeHistoryRepository;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class FakeTradeHistoryRepository implements TradeHistoryRepository {

	private final ConcurrentHashMap<Long, TradeHistory> storage = new ConcurrentHashMap<>();

	@Override
	public TradeHistory save(TradeHistory tradeHistory) {
		storage.put(tradeHistory.getId(), tradeHistory);
		return tradeHistory;
	}

	@Override
	public List<String> findDistinctCompanyCodes() {
		return storage.values().stream()
			.map(TradeHistory::getCompanyCode)
			.distinct()
			.collect(Collectors.toList());
	}

	@Override
	public List<TradeHistory> findRecentTradesByCompanyCode(String companyCode, Integer limit) {
		return storage.values().stream()
			.filter(trade -> trade.getCompanyCode().equals(companyCode))
			.sorted(Comparator.comparing(TradeHistory::getTradeTime).reversed())
			.limit(limit)
			.collect(Collectors.toList());
	}

	@Override
	public List<Object[]> findTotalTradeAmountByCompany(Pageable pageable) {
		// 테스트에 필요하지 않은 메서드는 빈 리스트 반환
		return new ArrayList<>();
	}

	@Override
	public List<Object[]> findTradeAvgPriceByCompany(Pageable pageable) {
		// 테스트에 필요하지 않은 메서드는 빈 리스트 반환
		return new ArrayList<>();
	}

	@Override
	public List<Object[]> findTradeCountByCompany(Pageable pageable) {
		// 테스트에 필요하지 않은 메서드는 빈 리스트 반환
		return new ArrayList<>();
	}

	// 테스트에 필요한 추가 메서드
	public Optional<TradeHistory> findById(Long id) {
		return Optional.ofNullable(storage.get(id));
	}

	public void clear() {
		storage.clear();
	}
}
