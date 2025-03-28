package com.onseju.orderservice.fake;

import com.onseju.orderservice.tradehistory.domain.TradeHistory;
import com.onseju.orderservice.tradehistory.repository.TradeHistoryJpaRepository;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.FluentQuery;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FakeTradeHistoryJpaRepository implements TradeHistoryJpaRepository {

	private final ConcurrentSkipListSet<TradeHistory> elements = new ConcurrentSkipListSet<>(
		Comparator.comparing(TradeHistory::getId));

	@Override
	public TradeHistory save(TradeHistory tradeHistory) {
		if (hasElement(tradeHistory)) {
			elements.stream()
				.filter(element -> element.getId().equals(tradeHistory.getId()))
				.forEach(elements::remove);
			elements.add(tradeHistory);
			return tradeHistory;
		}
		TradeHistory saved = TradeHistory.builder()
			.id((long)(elements.size() + 1))
			.companyCode(tradeHistory.getCompanyCode())
			.sellOrderId(tradeHistory.getSellOrderId())
			.buyOrderId(tradeHistory.getBuyOrderId())
			.price(tradeHistory.getPrice())
			.quantity(tradeHistory.getQuantity())
			.tradeTime(tradeHistory.getTradeTime())
			.build();
		elements.add(saved);
		return saved;
	}

	private boolean hasElement(TradeHistory tradeHistory) {
		if (tradeHistory.getId() == null) {
			return false;
		}
		return elements.stream()
			.anyMatch(o -> o.getId().equals(tradeHistory.getId()));
	}

	@Override
	public List<String> findDistinctCompanyCodes() {
		return elements.stream()
			.map(TradeHistory::getCompanyCode)
			.distinct()
			.collect(Collectors.toList());
	}

	@Override
	public List<TradeHistory> findRecentTradesByCompanyCode(String companyCode, Pageable pageable) {
		return elements.stream()
			.filter(t -> t.getCompanyCode().equals(companyCode))
			.sorted(Comparator.comparing(TradeHistory::getTradeTime).reversed())
			.limit(pageable.getPageSize())
			.collect(Collectors.toList());
	}

	@Override
	public List<Object[]> findTotalTradeAmountByCompany(Pageable pageable) {
		return elements.stream()
			.collect(Collectors.groupingBy(TradeHistory::getCompanyCode,
				Collectors.reducing(BigDecimal.ZERO,
					t -> t.getPrice().multiply(t.getQuantity()),
					BigDecimal::add)))
			.entrySet().stream()
			.sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
			.limit(pageable.getPageSize())
			.map(e -> new Object[]{e.getKey(), e.getValue()})
			.collect(Collectors.toList());
	}

	@Override
	public List<Object[]> findTradeAvgPriceByCompany(Pageable pageable) {
		return elements.stream()
			.collect(Collectors.groupingBy(TradeHistory::getCompanyCode,
				Collectors.averagingDouble(t -> t.getPrice().doubleValue())))
			.entrySet().stream()
			.sorted(Map.Entry.<String, Double>comparingByValue().reversed())
			.limit(pageable.getPageSize())
			.map(e -> new Object[]{e.getKey(), BigDecimal.valueOf(e.getValue())})
			.collect(Collectors.toList());
	}

	@Override
	public List<Object[]> findTradeCountByCompany(Pageable pageable) {
		return elements.stream()
			.collect(Collectors.groupingBy(TradeHistory::getCompanyCode, Collectors.counting()))
			.entrySet().stream()
			.sorted(Map.Entry.<String, Long>comparingByValue().reversed())
			.limit(pageable.getPageSize())
			.map(e -> new Object[]{e.getKey(), e.getValue()})
			.collect(Collectors.toList());
	}

	// 이하 JpaRepository의 나머지 메서드 구현

	@Override
	public <S extends TradeHistory> List<S> saveAll(Iterable<S> entities) {
		List<S> result = new ArrayList<>();
		entities.forEach(entity -> result.add((S) save(entity)));
		return result;
	}

	@Override
	public Optional<TradeHistory> findById(Long id) {
		return elements.stream()
			.filter(o -> o.getId().equals(id))
			.findFirst();
	}

	@Override
	public boolean existsById(Long id) {
		return elements.stream().anyMatch(o -> o.getId().equals(id));
	}

	@Override
	public List<TradeHistory> findAll() {
		return new ArrayList<>(elements);
	}

	@Override
	public List<TradeHistory> findAllById(Iterable<Long> ids) {
		Set<Long> idSet = new HashSet<>();
		ids.forEach(idSet::add);
		return elements.stream()
			.filter(o -> idSet.contains(o.getId()))
			.collect(Collectors.toList());
	}

	@Override
	public long count() {
		return elements.size();
	}

	@Override
	public void deleteById(Long id) {
		elements.removeIf(o -> o.getId().equals(id));
	}

	@Override
	public void delete(TradeHistory entity) {
		elements.remove(entity);
	}

	@Override
	public void deleteAllById(Iterable<? extends Long> ids) {
		Set<Long> idSet = new HashSet<>();
		ids.forEach(idSet::add);
		elements.removeIf(o -> idSet.contains(o.getId()));
	}

	@Override
	public void deleteAll(Iterable<? extends TradeHistory> entities) {
		entities.forEach(this::delete);
	}

	@Override
	public void deleteAll() {
		elements.clear();
	}

	@Override
	public List<TradeHistory> findAll(Sort sort) {
		// 정렬 구현은 복잡할 수 있으므로 간단하게 처리
		return findAll();
	}

	@Override
	public Page<TradeHistory> findAll(Pageable pageable) {
		List<TradeHistory> content = elements.stream()
			.skip(pageable.getOffset())
			.limit(pageable.getPageSize())
			.collect(Collectors.toList());
		return new PageImpl<>(content, pageable, elements.size());
	}

	@Override
	public <S extends TradeHistory> Optional<S> findOne(Example<S> example) {
		return (Optional<S>) elements.stream()
			.filter(o -> matches(o, example.getProbe()))
			.findFirst();
	}

	@Override
	public <S extends TradeHistory> List<S> findAll(Example<S> example) {
		return (List<S>) elements.stream()
			.filter(o -> matches(o, example.getProbe()))
			.collect(Collectors.toList());
	}

	@Override
	public <S extends TradeHistory> List<S> findAll(Example<S> example, Sort sort) {
		return findAll(example);
	}

	@Override
	public <S extends TradeHistory> Page<S> findAll(Example<S> example, Pageable pageable) {
		List<S> content = (List<S>) elements.stream()
			.filter(o -> matches(o, example.getProbe()))
			.skip(pageable.getOffset())
			.limit(pageable.getPageSize())
			.collect(Collectors.toList());
		return new PageImpl<>(content, pageable, content.size());
	}

	@Override
	public <S extends TradeHistory> long count(Example<S> example) {
		return elements.stream()
			.filter(o -> matches(o, example.getProbe()))
			.count();
	}

	@Override
	public <S extends TradeHistory> boolean exists(Example<S> example) {
		return elements.stream()
			.anyMatch(o -> matches(o, example.getProbe()));
	}

	@Override
	public <S extends TradeHistory, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
		// 이 메서드는 복잡하므로 간단한 구현으로 대체
		return null;
	}

	// 예제 매칭을 위한 헬퍼 메서드
	private boolean matches(TradeHistory entity, TradeHistory probe) {
		if (probe.getId() != null && !probe.getId().equals(entity.getId())) {
			return false;
		}
		if (probe.getCompanyCode() != null && !probe.getCompanyCode().equals(entity.getCompanyCode())) {
			return false;
		}
		// 나머지 필드에 대한 매칭 로직 추가
		return true;
	}

	// 추가 메서드들은 필요에 따라 구현
	@Override
	public void flush() {
		// 메모리 기반 저장소이므로 아무 작업도 수행하지 않음
	}

	@Override
	public <S extends TradeHistory> S saveAndFlush(S entity) {
		return (S)save(entity);
	}

	@Override
	public <S extends TradeHistory> List<S> saveAllAndFlush(Iterable<S> entities) {
		return saveAll(entities);
	}

	@Override
	public void deleteAllInBatch(Iterable<TradeHistory> entities) {
		deleteAll(entities);
	}

	@Override
	public void deleteAllByIdInBatch(Iterable<Long> ids) {
		deleteAllById(ids);
	}

	@Override
	public void deleteAllInBatch() {
		deleteAll();
	}

	@Override
	public TradeHistory getOne(Long id) {
		return findById(id).orElse(null);
	}

	@Override
	public TradeHistory getById(Long id) {
		return findById(id).orElse(null);
	}

	@Override
	public TradeHistory getReferenceById(Long id) {
		return findById(id).orElse(null);
	}
}
