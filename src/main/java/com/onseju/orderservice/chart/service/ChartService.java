package com.onseju.orderservice.chart.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.onseju.orderservice.chart.domain.TimeFrame;
import com.onseju.orderservice.chart.dto.CandleDto;
import com.onseju.orderservice.chart.dto.ChartResponseDto;
import com.onseju.orderservice.chart.dto.ChartUpdateDto;
import com.onseju.orderservice.tradehistory.domain.TradeHistory;
import com.onseju.orderservice.tradehistory.service.repository.TradeHistoryRepository;

import io.micrometer.common.util.StringUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 차트 데이터 관리 서비스
 * TradeHistoryService에서 분리된 차트 관련 기능
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChartService {

	private final SimpMessagingTemplate messagingTemplate;
	private final TradeHistoryRepository tradeHistoryRepository;

	// 상수 정의
	private static final Integer MAX_TRADE_HISTORY = 1000; // 종목당 최대 보관 거래 수
	private static final Integer CANDLE_KEEP_NUMBER = 100; // 캔들 데이터 보관 개수
	private static final double DEFAULT_PRICE = 100; // 기본 가격
	private static final String CHART_TOPIC_FORMAT = "/topic/chart/%s";
	private static final String TIMEFRAME_CHART_TOPIC_FORMAT = "/topic/chart/%s/%s";

	// 메모리 저장소
	private final Map<String, ConcurrentLinkedQueue<TradeHistory>> recentTradesMap = new ConcurrentHashMap<>();
	private final Map<String, Map<TimeFrame, List<CandleDto>>> timeFrameCandleMap = new ConcurrentHashMap<>();

	// 동시성 제어를 위한 락
	private final Map<String, ReentrantReadWriteLock> companyLocks = new ConcurrentHashMap<>();

	/**
	 * 서버 시작 시 거래 내역 로드 및 캔들 초기화
	 */
	@PostConstruct
	public void loadTradeHistoryFromDb() {
		log.info("서버 시작 시 DB에서 거래 내역 로드 중..");
		try {
			// 활성화된 모든 회사 코드 목록 조회 (거래 내역이 있는 종목 코드로 가정)
			final List<String> companyCodesWithTradeHistory = fetchActiveCompanyCodes();
			if (companyCodesWithTradeHistory.isEmpty()) {
				log.info("활성화된 종목 코드가 없습니다. 거래 내역 로드를 건너뜁니다.");
				return;
			}

			loadTradeHistoriesAndInitializeLocks(companyCodesWithTradeHistory);

			// 로드된 거래 내역 기반으로 캔들 초기화
			initializeCandlesFromTrades();
		} catch (Exception e) {
			log.error("거래 내역 로드 중 오류 발생");
		}
	}

	/**
	 * 활성화된 모든 종목 코드 목록 조회
	 */
	private List<String> fetchActiveCompanyCodes() {
		final List<String> companyCodes = tradeHistoryRepository.findDistinctCompanyCodes();
		log.debug("활성화된 종목 코드 수: {}", companyCodes.size());
		return companyCodes;
	}

	/**
	 * 각 종목별 거래 내역 로드 및 락 초기화
	 */
	private void loadTradeHistoriesAndInitializeLocks(final List<String> companyCodes) {
		for (String companyCode : companyCodes) {
			loadRecentTradesForCompany(companyCode);
			initializeLockForCompany(companyCode);
		}
	}

	/**
	 * 종목별 락 객체 초기회
	 */
	private void initializeLockForCompany(final String companyCode) {
		companyLocks.putIfAbsent(companyCode, new ReentrantReadWriteLock());
	}

	/**
	 * 특정 종목의 최근 거래 내역 로드
	 */
	private void loadRecentTradesForCompany(final String companyCode) {
		try {
			// 최근 거래 내역 MAX_TRADE_HISTORY 개 조회
			final List<TradeHistory> recentTrades = fetchRecentTradesForCompany(companyCode);
			if (recentTrades.isEmpty()) {
				log.debug("종목 {}의 거래 내역이 없습니다.", companyCode);
				return;
			}

			sortTradesByTimeDescending(recentTrades);
			storeTradesInMemory(companyCode, recentTrades);

			log.debug("종목 {}의 거래 내역 {}개 로드 완료", companyCode, recentTrades.size());
		} catch (Exception e) {
			log.error("거래 내역 로드 중 오류 발생");
		}
	}

	/**
	 * 종목별 최근 거래 내역 조회
	 */
	private List<TradeHistory> fetchRecentTradesForCompany(final String companyCode) {
		return tradeHistoryRepository.findRecentTradesByCompanyCode(companyCode, MAX_TRADE_HISTORY);
	}


	/**
	 * 거래 내역 시간 기준 내림차순 정렬 (최신순)
	 */
	private void sortTradesByTimeDescending(final List<TradeHistory> trades) {
		trades.sort((t1, t2) -> Long.compare(t2.getTradeTime(), t1.getTradeTime()));
	}

	/**
	 * 거래 내역을 메모리에 저장
	 */
	private void storeTradesInMemory(final String companyCode, final List<TradeHistory> trades) {
		final ConcurrentLinkedQueue<TradeHistory> tradeQueue = new ConcurrentLinkedQueue<>(trades);
		recentTradesMap.put(companyCode, tradeQueue);
	}

	/**
	 * 로드된 거래 내역 기반으로 캔들 초기화
	 */
	private void initializeCandlesFromTrades() {
		log.info("로드된 거래 내역을 기반으로 캔들 데이터 초기화 중...");

		for (Map.Entry<String, ConcurrentLinkedQueue<TradeHistory>> entry : recentTradesMap.entrySet()) {
			final String companyCode = entry.getKey();
			final Queue<TradeHistory> trades = entry.getValue();

			if (trades == null || trades.isEmpty()) {
				log.debug("종목 {}의 거래 내역이 없어 캔들 초기화를 건너뜁니다.", companyCode);
				continue;
			}

			final List<TradeHistory> sortedTrades = prepareTradesForCandleGeneration(trades);
			initializeAllTimeFrameCandles(companyCode, sortedTrades);
		}
	}

	/**
	 * 캔들 생성을 위한 거래 내역 준비 (시간 오름차순 정렬)
	 */
	private List<TradeHistory> prepareTradesForCandleGeneration(final Queue<TradeHistory> trades) {
		final List<TradeHistory> tradesList = new ArrayList<>(trades);
		tradesList.sort(Comparator.comparingLong(TradeHistory::getTradeTime));
		return tradesList;
	}

	/**
	 * 모든 타임프레임의 캔들 초기화
	 */
	private void initializeAllTimeFrameCandles(final String companyCode, final List<TradeHistory> sortedTrades) {
		for (TimeFrame timeFrame : TimeFrame.values()) {
			initializeTimeFrameCandles(companyCode, timeFrame, sortedTrades);
		}
	}

	/**
	 * 특정 타임프레임의 캔들 초기화
	 */
	private void initializeTimeFrameCandles(
			final String companyCode,
			final TimeFrame timeFrame,
			final List<TradeHistory> trades) {

		if (trades.isEmpty()) {
			log.debug("종목 {}의 {}분봉 캔들 초기화: 거래 내역이 없음", companyCode, timeFrame.getTimeCode());
			return;
		}

		// 타임프레임별 캔들 데이터 초기화
		final Map<TimeFrame, List<CandleDto>> companyCodeCandleMap = timeFrameCandleMap
				.computeIfAbsent(companyCode, k -> new EnumMap<>(TimeFrame.class));

		// 캔들 리스트 초기화
		final List<CandleDto> candles = new ArrayList<>();
		companyCodeCandleMap.put(timeFrame, candles);

		generateCandlesFromTrades(candles, trades, timeFrame);
		fillEmptyCandlesUntilNow(candles, timeFrame);
		limitCandleListSize(companyCodeCandleMap, timeFrame, candles);
	}

	/**
	 * 거래 내역으로부터 캔들 생성
	 */
	private void generateCandlesFromTrades(
			final List<CandleDto> candles,
			final List<TradeHistory> trades,
			final TimeFrame timeFrame
	) {
		final Long timeFrameSeconds = timeFrame.getSeconds();
		Long currentCandleTime = null;
		Double open = null, high = null, low = null, close = null;
		Integer volume = 0;

		for (TradeHistory trade : trades) {
			final Long tradeTime = trade.getTradeTime();
			final Double price = trade.getPrice().doubleValue();
			final Integer tradeVolume = trade.getQuantity().intValue();
			final Long candleTime = calculateCandleTime(tradeTime, timeFrameSeconds);

			if (currentCandleTime == null || candleTime > currentCandleTime) {
				if (currentCandleTime != null) {
					fillEmptyCandles(candles, currentCandleTime, candleTime, timeFrameSeconds, close);
					candles.add(createCandleDto(currentCandleTime, open, high, low, close, volume));
				}

				// 새 캔들 시작
				currentCandleTime = candleTime;
				open = high = low = close = price;
				volume = tradeVolume;
			} else {
				// 같은 캔들 시간의 거래인 경우 업데이트
				high = Math.max(high, price);
				low = Math.min(low, price);
				close = price;
				volume += tradeVolume;
			}
		}

		// 마지막 캔들 추가
		if (currentCandleTime != null) {
			candles.add(createCandleDto(currentCandleTime, open, high, low, close, volume));
		}
	}

	/**
	 * 현재 시간까지 빈 캔들 채우기
	 */
	private void fillEmptyCandlesUntilNow(final List<CandleDto> candles, final TimeFrame timeFrame) {
		if (candles.isEmpty()) {
			return;
		}

		final Long now = Instant.now().getEpochSecond();
		final Long lastCandleTime = candles.get(candles.size() - 1).time();
		final Long timeFrameSeconds = timeFrame.getSeconds();
		final Double lastPrice = candles.get(candles.size() - 1).close();

		fillEmptyCandles(candles, lastCandleTime, calculateCandleTime(now, timeFrameSeconds),
				timeFrameSeconds, lastPrice);
	}

	/**
	 * 캔들 시간 계산 (타임프레임 단위로 내림)
	 */
	private Long calculateCandleTime(final Long timeInSeconds, final Long timeFrameSeconds) {
		return timeInSeconds - (timeInSeconds % timeFrameSeconds);
	}

	/**
	 * 빈 캔들 채우기
	 */
	private void fillEmptyCandles(
			final List<CandleDto> candles,
			final Long fromTime,
			final Long toTime,
			final Long timeFrameSeconds,
			final Double lastPrice) {
		// 인자 유효성 검증
		if (lastPrice == null || fromTime == null || toTime == null || fromTime >= toTime) {
			return;
		}

		// 기본값 설정
		final Double safePriceValue = !Double.isNaN(lastPrice) ? lastPrice : DEFAULT_PRICE;

		// 캔들 채우기
		for (Long time = fromTime + timeFrameSeconds; time < toTime; time += timeFrameSeconds) {
			candles.add(createCandleDto(time, safePriceValue, safePriceValue, safePriceValue, safePriceValue, 0));
		}
	}

	/**
	 * CandleDto 생성 (null 값 방지)
	 */
	private CandleDto createCandleDto(
			Long time,
			final Double open,
			final Double high,
			final Double low,
			final Double close,
			final Integer volume
	) {
		// 시간 값이 유효하지 않은 경우 로그 남기고 현재 시간으로 대체
		if (time == null || time <= 0) {
			log.warn("유효하지 않은 candle 시간값, 현재 시간으로 대체합니다.");
			time = Instant.now().getEpochSecond();
		}

		// 가격 유효성 검증 및 기본값 설정
		final Double safeOpen = validatePrice(open);
		Double safeHigh = validatePrice(high);
		Double safeLow = validatePrice(low);
		final Double safeClose = validatePrice(close);
		final Integer safeVolume = (volume != null && volume >= 0) ? volume : 0;

		// 최대/최소값 논리적 검증
		safeHigh = Math.max(Math.max(safeOpen, safeClose), safeLow);
		safeLow = Math.min(Math.min(safeOpen, safeClose), safeHigh);

		return CandleDto.builder()
				.time(time)
				.open(safeOpen)
				.high(safeHigh)
				.low(safeLow)
				.close(safeClose)
				.volume(safeVolume)
				.build();
	}

	/**
	 * 가격 유효성 검증
	 */
	private Double validatePrice(final Double price) {
		return (price != null && !price.isNaN()) ? price : DEFAULT_PRICE;
	}

	/**
	 * 모든 타임프레임의 캔들 데이터 업데이트
	 */
	public void updateCandles(final String companyCode) {
		if (StringUtils.isBlank(companyCode)) {
			log.warn("캔들 업데이트 실패: 종목코드가 유효하지 않습니다.");
			return;
		}
		Arrays.stream(TimeFrame.values())
				.forEach(timeFrame -> updateTimeFrameCandle(companyCode, timeFrame));
	}

	/**
	 * 특정 타임프레임 캔들 데이터 업데이트 (예외 처리 포함)
	 */
	private void updateTimeFrameCandle(final String companyCode, final TimeFrame timeFrame) {
		try {
			updateCandlesForTimeFrame(companyCode, timeFrame);
		} catch (Exception e) {
			log.error("종목 {}의 {} 타임프레임 캔들 업데이트 중 오류 발생: {}",
					companyCode, timeFrame.getTimeCode(), e.getMessage(), e);
		}
	}

	/**
	 * 특정 타임프레임 캔들 데이터 업데이트 구현
	 */
	private void updateCandlesForTimeFrame(final String companyCode, final TimeFrame timeFrame) {
		ReentrantReadWriteLock lock = acquireWriteLock(companyCode);
		try {
			final Map<TimeFrame, List<CandleDto>> companyTimeFrameMap = getOrCreateCompanyTimeFrameMap(companyCode);
			final List<CandleDto> candles = getOrCreateCandlesList(companyTimeFrameMap, timeFrame);

			final Long currentCandleTime = calculateCurrentCandleTime(timeFrame);

			updateCandlesBasedOnCurrentState(companyCode, timeFrame, companyTimeFrameMap, candles, currentCandleTime);
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * 종목별 쓰기 락 획득
	 */
	private ReentrantReadWriteLock acquireWriteLock(final String companyCode) {
		ReentrantReadWriteLock lock = companyLocks.computeIfAbsent(
				companyCode, k -> new ReentrantReadWriteLock());
		lock.writeLock().lock();
		return lock;
	}

	/**
	 * 종목별 타임프레임 맵 조회 또는 생성
	 */
	private Map<TimeFrame, List<CandleDto>> getOrCreateCompanyTimeFrameMap(final String companyCode) {
		return timeFrameCandleMap.computeIfAbsent(
				companyCode, k -> new EnumMap<>(TimeFrame.class));
	}

	/**
	 * 타임프레임별 캔들 리스트 조회 또는 생성
	 */
	private List<CandleDto> getOrCreateCandlesList(
			final Map<TimeFrame, List<CandleDto>> companyTimeFrameMap,
			final TimeFrame timeFrame) {
		return companyTimeFrameMap.computeIfAbsent(
				timeFrame, k -> new ArrayList<>());
	}

	/**
	 * 현재 시간에 해당하는 캔들 시간 계산
	 */
	private Long calculateCurrentCandleTime(final TimeFrame timeFrame) {
		final Long now = Instant.now().getEpochSecond();
		return calculateCandleTime(now, timeFrame.getSeconds());
	}

	/**
	 * 캔들 현재 상태에 따른 업데이트 로직
	 */
	private void updateCandlesBasedOnCurrentState(
			final String companyCode,
			final TimeFrame timeFrame,
			final Map<TimeFrame, List<CandleDto>> companyTimeFrameMap,
			final List<CandleDto> candles,
			final Long currentCandleTime
	) {
		if (candles.isEmpty()) {
			addNewCandleForEmptyList(companyCode, candles, currentCandleTime);
		} else {
			updateExistingCandles(companyCode, timeFrame, companyTimeFrameMap, candles, currentCandleTime);
		}

	}

	/**
	 * 빈 캔들 리스트에 새 캔들 추가
	 */
	private void addNewCandleForEmptyList(
			final String companyCode,
			final List<CandleDto> candles,
			final Long currentCandleTime
	) {
		final Double lastPrice = getLastPrice(companyCode);
		final CandleDto newCandle = createCandleDto(currentCandleTime, lastPrice, lastPrice, lastPrice, lastPrice, 0);
		candles.add(newCandle);
	}

	/**
	 * 기존 캔들 리스트 업데이트
	 */
	private void updateExistingCandles(
			final String companyCode,
			final TimeFrame timeFrame,
			final Map<TimeFrame, List<CandleDto>> companyTimeFrameMap,
			final List<CandleDto> candles,
			final Long currentCandleTime
	) {
		final CandleDto lastCandle = candles.get(candles.size() - 1);

		if (!lastCandle.time().equals(currentCandleTime)) {
			handleDifferentCandleTime(
					companyCode, timeFrame, companyTimeFrameMap, candles, lastCandle, currentCandleTime);
		} else {
			log.debug("종목 {}의 {}분봉 캔들 업데이트: 현재 캔들 시간과 마지막 캔들 시간이 동일 ({})",
					companyCode, timeFrame.getTimeCode(), Instant.ofEpochSecond(currentCandleTime));
		}
	}

	/**
	 * 마지막 캔들과 다른 시간의 캔들 처리
	 */
	private void handleDifferentCandleTime(
			final String companyCode,
			final TimeFrame timeFrame,
			final Map<TimeFrame, List<CandleDto>> companyTimeFrameMap,
			final List<CandleDto> candles,
			final CandleDto lastCandle,
			final Long currentCandleTime
	) {
		// 빈 캔들 채우기
		fillEmptyCandles(candles, lastCandle.time(), currentCandleTime, timeFrame.getSeconds(), lastCandle.close());

		// 새 캔들 시간이면 추가
		if (currentCandleTime > lastCandle.time()) {
			addNewCandle(candles, currentCandleTime, lastCandle.close());
		}

		// 캔들 개수 제한
		limitCandleListSize(companyTimeFrameMap, timeFrame, candles);
	}

	/**
	 * 새 캔들 생성 및 추가
	 */
	private void addNewCandle(final List<CandleDto> candles, final Long candleTime, final Double price) {
		final CandleDto newCandle = createCandleDto(candleTime, price, price, price, price, 0);
		candles.add(newCandle);
	}

	/**
	 * 캔들 목록 크기 제한
	 */
	private void limitCandleListSize(
			final Map<TimeFrame, List<CandleDto>> companyCodeCandleMap,
			final TimeFrame timeFrame,
			final List<CandleDto> candles
	) {
		if (candles.size() > CANDLE_KEEP_NUMBER) {
			final List<CandleDto> limitedCandles = new ArrayList<>(
					candles.subList(candles.size() - CANDLE_KEEP_NUMBER, candles.size()));
			companyCodeCandleMap.put(timeFrame, limitedCandles);
		}
	}

	/**
	 * 마지막 거래 가격 조회
	 */
	private Double getLastPrice(final String companyCode) {
		ConcurrentLinkedQueue<TradeHistory> trades = recentTradesMap.get(companyCode);
		if (trades == null || trades.isEmpty()) {
			return DEFAULT_PRICE;
		}
		return trades.peek().getPrice().doubleValue();
	}

	/**
	 * 거래 내역 메모리 저장
	 */
	public void storeTradeHistory(final TradeHistory tradeHistory) {
		if (tradeHistory == null || tradeHistory.getCompanyCode() == null) {
			log.warn("거래 내역 저장 실패: 유효하지 않은 거래 내역");
			return;
		}

		final String companyCode = tradeHistory.getCompanyCode();
		final ConcurrentLinkedQueue<TradeHistory> trades = recentTradesMap.computeIfAbsent(
				companyCode, k -> new ConcurrentLinkedQueue<>());

		trades.offer(tradeHistory);

		// 최대 개수 유지
		enforceMaxTradeHistoryLimit(trades);
	}

	/**
	 * 거래내역 최대 개수 유지
	 */
	private void enforceMaxTradeHistoryLimit(ConcurrentLinkedQueue<TradeHistory> trades) {
		while (trades.size() > MAX_TRADE_HISTORY) {
			trades.poll();
		}
	}

	/**
	 * 마지막 거래 조회
	 */
	public Optional<TradeHistory> getLastTrade(final String companyCode) {
		if (StringUtils.isBlank(companyCode)) {
			return Optional.empty();
		}

		final Queue<TradeHistory> trades = recentTradesMap.get(companyCode);
		return trades == null || trades.isEmpty() ?
				Optional.empty() : Optional.of(trades.peek());
	}

	/**
	 * 모든 타임프레임 캔들 데이터 거래 기반 업데이트
	 */
	public void updateAllTimeFrameCandles(final TradeHistory tradeHistory) {
		if (tradeHistory == null || tradeHistory.getCompanyCode() == null) {
			log.warn("캔들 업데이트 실패: 유효하지 않은 거래 내역");
			return;
		}

		final String companyCode = tradeHistory.getCompanyCode();
		final ReentrantReadWriteLock lock = acquireWriteLock(companyCode);

		try {
			final Map<TimeFrame, List<CandleDto>> companyTimeFrameMap = getOrCreateCompanyTimeFrameMap(companyCode);
			for (TimeFrame timeFrame : TimeFrame.values()) {
				updateCandleWithTradeForTimeFrame(tradeHistory, companyTimeFrameMap, timeFrame);
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * 특정 타임프레임 캔들 데이터 거래 기반 업데이트
	 */
	private void updateCandleWithTradeForTimeFrame(
			final TradeHistory tradeHistory,
			final Map<TimeFrame, List<CandleDto>> companyTimeFrameMap,
			final TimeFrame timeFrame
	) {

		final List<CandleDto> candles = companyTimeFrameMap.computeIfAbsent(
				timeFrame, k -> Collections.emptyList());

		final Long tradeTime = tradeHistory.getTradeTime();
		final Long timeFrameSeconds = timeFrame.getSeconds();
		final Long candleTime = calculateCandleTime(tradeTime, timeFrameSeconds);

		final Double price = extractSafePrice(tradeHistory);
		final Integer volume = extractSafeVolume(tradeHistory);

		if (candles.isEmpty()) {
			handleEmptyCandleList(candles, candleTime, price, volume);
		} else {
			updateExistingCandleList(
					timeFrame, companyTimeFrameMap, candles, candleTime, price, volume, timeFrameSeconds);
		}
	}

	/**
	 * 거래 가격 안전하게 추출
	 */
	private Double extractSafePrice(final TradeHistory tradeHistory) {
		return tradeHistory.getPrice() != null ?
				tradeHistory.getPrice().doubleValue() : DEFAULT_PRICE;
	}

	/**
	 * 거래량 안전하게 추출
	 */
	private Integer extractSafeVolume(final TradeHistory tradeHistory) {
		return tradeHistory.getQuantity() != null ?
				tradeHistory.getQuantity().intValue() : 0;
	}

	/**
	 * 빈 캔들 리스트에 거래 기반 캔들 추가
	 */
	private void handleEmptyCandleList(
			final List<CandleDto> candles,
			final Long candleTime,
			final Double price,
			final Integer volume
	) {
		final CandleDto newCandle = createCandleDto(candleTime, price, price, price, price, volume);
		candles.add(newCandle);
	}

	/**
	 * 기존 캔들 리스트 거래 기반 업데이트
	 */
	private void updateExistingCandleList(
			final TimeFrame timeFrame,
			final Map<TimeFrame, List<CandleDto>> companyTimeFrameMap,
			final List<CandleDto> candles,
			final Long candleTime,
			final Double price,
			final Integer volume,
			final Long timeFrameSeconds
	) {
		final CandleDto lastCandle = candles.get(candles.size() - 1);

		if (lastCandle.time().equals(candleTime)) {
			updateExistingCandle(candles, lastCandle, price, volume);
		} else if (candleTime > lastCandle.time()) {
			handleNewCandleTime(
					timeFrame, companyTimeFrameMap, candles, lastCandle, candleTime, price, volume, timeFrameSeconds);
		}
	}

	/**
	 * 기존 캔들 업데이트
	 */
	private void updateExistingCandle(
			final List<CandleDto> candles,
			final CandleDto lastCandle,
			final Double price,
			final Integer volume
	) {
		final CandleDto updatedCandle = CandleDto.builder()
				.time(lastCandle.time())
				.open(lastCandle.open())
				.high(Math.max(lastCandle.high(), price))
				.low(Math.min(lastCandle.low(), price))
				.close(price)
				.volume(lastCandle.volume() + volume)
				.build();

		candles.set(candles.size() - 1, updatedCandle);
	}

	/**
	 * 새로운 캔들 시간 처리
	 */
	private void handleNewCandleTime(
			final TimeFrame timeFrame,
			final Map<TimeFrame, List<CandleDto>> companyTimeFrameMap,
			final List<CandleDto> candles,
			final CandleDto lastCandle,
			final Long candleTime,
			final Double price,
			final Integer volume,
			final Long timeFrameSeconds
	) {

		// 빈 캔들 채우기
		fillEmptyCandles(candles, lastCandle.time(), candleTime, timeFrameSeconds, lastCandle.close());

		// 새 캔들 생성
		final CandleDto newCandle = createCandleDto(candleTime, price, price, price, price, volume);
		candles.add(newCandle);

		// 캔들 리스트 크기 제한
		limitCandleListSize(companyTimeFrameMap, timeFrame, candles);
	}

	/**
	 * 차트 업데이트 전송
	 */
	public void sendChartUpdates(final TradeHistory tradeHistory) {
		if (tradeHistory == null || tradeHistory.getCompanyCode() == null) {
			log.warn("차트 업데이트 전송 실패: 유효하지 않은 거래 내역");
			return;
		}

		final String companyCode = tradeHistory.getCompanyCode();
		final Double price = extractSafePrice(tradeHistory);
		final Integer volume = extractSafeVolume(tradeHistory);

		// 기본 업데이트 전송
		sendBasicChartUpdate(companyCode, price, volume);

		// 타임프레임별 업데이트 전송
		sendTimeFrameChartUpdates(companyCode);
	}

	/**
	 * 기본 차트 업데이트 전송
	 */
	private void sendBasicChartUpdate(final String companyCode, final Double price, final Integer volume) {
		final ChartUpdateDto basicUpdateDto = ChartUpdateDto.builder()
				.price(price)
				.volume(volume)
				.build();

		messagingTemplate.convertAndSend(String.format(CHART_TOPIC_FORMAT, companyCode), basicUpdateDto);
	}

	/**
	 * 타임프레임별 차트 업데이트 전송
	 */
	private void sendTimeFrameChartUpdates(final String companyCode) {
		final ReentrantReadWriteLock lock = companyLocks.computeIfAbsent(
				companyCode, k -> new ReentrantReadWriteLock());
		lock.readLock().lock();

		try {
			final Map<TimeFrame, List<CandleDto>> companyTimeFrameMap = timeFrameCandleMap.get(companyCode);
			if (companyTimeFrameMap == null) {
				return;
			}

			for (TimeFrame timeFrame : TimeFrame.values()) {
				sendTimeFrameUpdate(companyCode, companyTimeFrameMap, timeFrame);
			}
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * 특정 타임프레임 업데이트 전송
	 */
	private void sendTimeFrameUpdate(
			final String companyCode,
			final Map<TimeFrame, List<CandleDto>> companyTimeFrameMap,
			final TimeFrame timeFrame
	) {
		final List<CandleDto> candles = companyTimeFrameMap.get(timeFrame);
		if (candles == null || candles.isEmpty()) {
			return;
		}

		final CandleDto latestCandle = candles.get(candles.size() - 1);
		if (latestCandle == null) {
			return;
		}

		final ChartUpdateDto timeFrameUpdateDto = ChartUpdateDto.builder()
				.price(latestCandle.close())
				.volume(latestCandle.volume())
				.timeCode(timeFrame.getTimeCode())
				.build();

		final String destination = String.format(TIMEFRAME_CHART_TOPIC_FORMAT, companyCode, timeFrame.getTimeCode());
		messagingTemplate.convertAndSend(destination, timeFrameUpdateDto);
	}

	/**
	 * 차트 기록 조회
	 */
	public ChartResponseDto getChartHistory(final String companyCode, final String timeframeCode) {
		if (StringUtils.isBlank(companyCode) || StringUtils.isBlank(timeframeCode)) {
			log.warn("차트 기록 조회 실패: 유효하지 않은 파라미터");
			return createEmptyChartResponse(TimeFrame.MINUTE_15.getTimeCode());
		}

		final TimeFrame requestedTimeFrame = findTimeFrameByCode(timeframeCode);
		final ReentrantReadWriteLock lock = acquireLockForReading(companyCode);

		try {
			final List<CandleDto> processedCandles = getCandlesForTimeFrame(companyCode, requestedTimeFrame);
			return createChartResponse(requestedTimeFrame, processedCandles);
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * 코드로 타임프레임 찾기
	 */
	private TimeFrame findTimeFrameByCode(final String timeframeCode) {
		return Arrays.stream(TimeFrame.values())
				.filter(tf -> tf.getTimeCode().equals(timeframeCode))
				.findFirst()
				.orElse(TimeFrame.MINUTE_15); // 기본값 15분
	}

	/**
	 * 읽기용 락 획득
	 */
	private ReentrantReadWriteLock acquireLockForReading(String companyCode) {
		final ReentrantReadWriteLock lock = companyLocks.computeIfAbsent(
				companyCode, k -> new ReentrantReadWriteLock());
		lock.readLock().lock();
		return lock;
	}

	/**
	 * 특정 타임프레임의 캔들 목록 조회
	 */
	private List<CandleDto> getCandlesForTimeFrame(final String companyCode, final TimeFrame timeFrame) {
		final List<CandleDto> timeFrameCandles = getTimeFrameCandles(companyCode, timeFrame);

		if (timeFrameCandles.isEmpty()) {
			return createDefaultCandleList(timeFrame);
		}

		return processAndConvertValidCandles(timeFrameCandles);
	}

	/**
	 * 종목과 타임프레임에 맞는 캔들 데이터 조회
	 */
	private List<CandleDto> getTimeFrameCandles(final String companyCode, final TimeFrame timeFrame) {
		final Map<TimeFrame, List<CandleDto>> companyTimeFrameMap = timeFrameCandleMap.get(companyCode);
		if (companyTimeFrameMap == null) {
			return Collections.emptyList();
		}

		final List<CandleDto> timeFrameCandles = companyTimeFrameMap.get(timeFrame);
		return timeFrameCandles != null ? timeFrameCandles : Collections.emptyList();
	}

	/**
	 * 유효한 캔들 데이터만 필터링, 정렬 후 변환
	 */
	private List<CandleDto> processAndConvertValidCandles(final List<CandleDto> candles) {
		// 유효한 캔들만 필터링
		final List<CandleDto> validCandles = new ArrayList<>(candles.stream()
				.filter(this::isValidCandle)
				.toList());

		if (validCandles.isEmpty()) {
			return new ArrayList<>();
		}

		// 시간순으로 정렬
		validCandles.sort(Comparator.comparingLong(CandleDto::time));

		// 정규화된 캔들 생성 (불변성을 보장하기 위해 새 객체 생성)
		return validCandles.stream()
				.map(candle -> createCandleDto(
						candle.time(),
						candle.open(),
						candle.high(),
						candle.low(),
						candle.close(),
						candle.volume()))
				.toList();
	}

	/**
	 * 캔들 유효성 검사
	 */
	private Boolean isValidCandle(final CandleDto candle) {
		return candle != null && candle.time() != null && candle.time() > 0;
	}

	/**
	 * 기본 캔들 목록 생성 (비어있을 경우)
	 */
	private List<CandleDto> createDefaultCandleList(final TimeFrame timeFrame) {
		final List<CandleDto> defaultList = new ArrayList<>();
		defaultList.add(createDefaultCandle(timeFrame));

		log.info("타임프레임 {}에 대한 캔들 데이터가 없어 기본 캔들을 생성합니다.",
				timeFrame.getTimeCode());

		return defaultList;
	}

	/**
	 * 기본 캔들 생성
	 */
	private CandleDto createDefaultCandle(final TimeFrame timeFrame) {
		final Long now = Instant.now().getEpochSecond();
		final Long timeFrameSeconds = timeFrame.getSeconds();
		final Long currentCandleTime = calculateCandleTime(now, timeFrameSeconds);

		return createCandleDto(
				currentCandleTime,
				DEFAULT_PRICE,
				DEFAULT_PRICE,
				DEFAULT_PRICE,
				DEFAULT_PRICE,
				0);
	}

	/**
	 * 차트 응답 DTO 생성
	 */
	private ChartResponseDto createChartResponse(final TimeFrame timeFrame, final List<CandleDto> candles) {
		return ChartResponseDto.builder()
				.candles(candles)
				.timeCode(timeFrame.getTimeCode())
				.build();
	}

	/**
	 * 빈 차트 응답 DTO 생성
	 */
	private ChartResponseDto createEmptyChartResponse(final String timeCode) {
		return ChartResponseDto.builder()
				.candles(Collections.emptyList())
				.timeCode(timeCode)
				.build();
	}

}
