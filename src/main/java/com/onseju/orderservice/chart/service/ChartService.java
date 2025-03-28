package com.onseju.orderservice.chart.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.onseju.orderservice.chart.domain.TimeFrame;
import com.onseju.orderservice.chart.dto.CandleDto;
import com.onseju.orderservice.chart.dto.ChartResponseDto;
import com.onseju.orderservice.chart.dto.ChartUpdateDto;
import com.onseju.orderservice.company.service.ClosingPriceService;
import com.onseju.orderservice.tradehistory.domain.TradeHistory;
import com.onseju.orderservice.tradehistory.service.repository.TradeHistoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 차트 데이터 관리 서비스 - 간소화 버전
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChartService {

	private final SimpMessagingTemplate messagingTemplate;
	private final TradeHistoryRepository tradeHistoryRepository;
	private final ClosingPriceService closingPriceService;

	// 상수
	private static final int MAX_TRADE_HISTORY = 1000;
	private static final int CANDLE_KEEP_NUMBER = 100;
	private static final String CHART_TOPIC_FORMAT = "/topic/chart/%s";
	private static final String TIMEFRAME_CHART_TOPIC_FORMAT = "/topic/chart/%s/%s";

	// 메모리 저장소
	private final Map<String, ConcurrentLinkedQueue<TradeHistory>> recentTradesMap = new ConcurrentHashMap<>();
	private final Map<String, Map<TimeFrame, List<CandleDto>>> timeFrameCandleMap = new ConcurrentHashMap<>();
	private final Map<String, ReentrantReadWriteLock> companyLocks = new ConcurrentHashMap<>();

	/**
	 * 서버 시작 시 차트 데이터 초기화
	 */
	public void initializeAllCompanyCandleData() {
		try {
			final Set<String> allCompanyCodes = closingPriceService.getAllCompanyCodeByInmemory();
			if (allCompanyCodes.isEmpty()) {
				return;
			}

			// 모든 종목에 대해 락 초기화
			allCompanyCodes.forEach(code -> companyLocks.putIfAbsent(code, new ReentrantReadWriteLock()));

			// 거래 내역 로드
			tradeHistoryRepository.findDistinctCompanyCodes().forEach(this::loadRecentTradesForCompany);

			// 캔들 초기화
			allCompanyCodes.forEach(this::initializeCandlesForCompany);
		} catch (Exception e) {
			log.error("차트 데이터 초기화 중 오류 발생", e);
		}
	}

	/**
	 * 특정 종목의 최근 거래 내역 로드
	 */
	private void loadRecentTradesForCompany(final String companyCode) {
		try {
			final List<TradeHistory> recentTrades =
					tradeHistoryRepository.findRecentTradesByCompanyCode(companyCode, MAX_TRADE_HISTORY);

			if (!recentTrades.isEmpty()) {
				recentTrades.sort(Comparator.comparing(TradeHistory::getTradeTime).reversed());
				recentTradesMap.put(companyCode, new ConcurrentLinkedQueue<>(recentTrades));
			}
		} catch (Exception e) {
			log.error("종목 {} 거래 내역 로드 중 오류 발생", companyCode, e);
		}
	}

	/**
	 * 특정 종목의 캔들 초기화
	 */
	private void initializeCandlesForCompany(final String companyCode) {
		final ReentrantReadWriteLock lock = companyLocks.get(companyCode);
		if (lock == null)
			return;

		lock.writeLock().lock();
		try {
			final ConcurrentLinkedQueue<TradeHistory> trades = recentTradesMap.get(companyCode);
			final Map<TimeFrame, List<CandleDto>> companyTimeFrameMap =
					timeFrameCandleMap.computeIfAbsent(companyCode, k -> new EnumMap<>(TimeFrame.class));

			// 각 타임프레임별 캔들 생성
			for (TimeFrame timeFrame : TimeFrame.values()) {
				List<CandleDto> candles = new ArrayList<>();
				companyTimeFrameMap.put(timeFrame, candles);

				if (trades != null && !trades.isEmpty()) {
					// 거래 내역 기반 캔들 생성
					generateCandlesFromTrades(candles, new ArrayList<>(trades), timeFrame);
				} else {
					// 기본 캔들 생성
					final Double price = closingPriceService.getClosingPrice(companyCode).doubleValue();
					final Long now = Instant.now().getEpochSecond();
					final Long candleTime = calculateCandleTime(now, timeFrame.getSeconds());
					candles.add(createCandle(candleTime, price, price, price, price, 0));
				}
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * 거래 내역으로부터 캔들 생성
	 */
	private void generateCandlesFromTrades(
			final List<CandleDto> candles,
			final List<TradeHistory> trades,
			final TimeFrame timeFrame) {

		if (trades.isEmpty())
			return;

		trades.sort(Comparator.comparing(TradeHistory::getTradeTime));
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
				// 이전 캔들 저장
				if (currentCandleTime != null) {
					candles.add(createCandle(currentCandleTime, open, high, low, close, volume));
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
			candles.add(createCandle(currentCandleTime, open, high, low, close, volume));
		}

		// 현재 시간까지 캔들 추가
		if (!candles.isEmpty()) {
			final Long now = Instant.now().getEpochSecond();
			final Long lastCandleTime = candles.get(candles.size() - 1).time();
			final Long candleTime = calculateCandleTime(now, timeFrameSeconds);
			final Double lastPrice = candles.get(candles.size() - 1).close();

			for (Long time = lastCandleTime + timeFrameSeconds; time <= candleTime; time += timeFrameSeconds) {
				candles.add(createCandle(time, lastPrice, lastPrice, lastPrice, lastPrice, 0));
			}
		}

		// 캔들 개수 제한
		if (candles.size() > CANDLE_KEEP_NUMBER) {
			List<CandleDto> limitedCandles = new ArrayList<>(
					candles.subList(candles.size() - CANDLE_KEEP_NUMBER, candles.size()));
			candles.clear();
			candles.addAll(limitedCandles);
		}
	}

	/**
	 * 현재 시간 기준으로 캔들 데이터 업데이트 (자동)
	 */
	private void updateCurrentCandles(final String companyCode, final TimeFrame timeFrame) {
		final ReentrantReadWriteLock lock = companyLocks.computeIfAbsent(
				companyCode, k -> new ReentrantReadWriteLock());

		lock.writeLock().lock();
		try {
			final Map<TimeFrame, List<CandleDto>> companyTimeFrameMap =
					timeFrameCandleMap.computeIfAbsent(companyCode, k -> new EnumMap<>(TimeFrame.class));
			final List<CandleDto> candles =
					companyTimeFrameMap.computeIfAbsent(timeFrame, k -> new ArrayList<>());

			// 캔들이 없으면 기본 캔들 생성
			if (candles.isEmpty()) {
				final Double price = closingPriceService.getClosingPrice(companyCode).doubleValue();
				final Long now = Instant.now().getEpochSecond();
				final Long candleTime = calculateCandleTime(now, timeFrame.getSeconds());
				candles.add(createCandle(candleTime, price, price, price, price, 0));
				return;
			}

			// 현재 시간 기준 새 캔들 추가 (필요시)
			final CandleDto lastCandle = candles.get(candles.size() - 1);
			final Long now = Instant.now().getEpochSecond();
			final Long candleTime = calculateCandleTime(now, timeFrame.getSeconds());

			if (candleTime > lastCandle.time()) {
				// 빈 캔들 채우기
				for (Long time = lastCandle.time() + timeFrame.getSeconds();
					 time <= candleTime;
					 time += timeFrame.getSeconds()) {
					candles.add(createCandle(time, lastCandle.close(), lastCandle.close(),
							lastCandle.close(), lastCandle.close(), 0));
				}

				// 캔들 개수 제한
				if (candles.size() > CANDLE_KEEP_NUMBER) {
					List<CandleDto> limitedCandles = new ArrayList<>(
							candles.subList(candles.size() - CANDLE_KEEP_NUMBER, candles.size()));
					candles.clear();
					candles.addAll(limitedCandles);
				}
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * 특정 종목의 모든 타임프레임 캔들 데이터 업데이트
	 */
	public void updateCandles(final String companyCode) {
		if (companyCode == null || companyCode.trim().isEmpty()) {
			log.warn("캔들 업데이트 실패: 종목코드가 유효하지 않습니다.");
			return;
		}

		for (TimeFrame timeFrame : TimeFrame.values()) {
			try {
				updateCurrentCandles(companyCode, timeFrame);
			} catch (Exception e) {
				log.error("종목 {}의 {}분봉 캔들 업데이트 중 오류 발생", companyCode, timeFrame.getTimeCode(), e);
			}
		}
	}

	/**
	 * 캔들 생성 헬퍼 메서드
	 */
	private CandleDto createCandle(
			Long time,
			final Double open,
			final Double high,
			final Double low,
			final Double close,
			final Integer volume) {

		return CandleDto.builder()
				.time(time)
				.open(open)
				.high(high)
				.low(low)
				.close(close)
				.volume(volume)
				.build();
	}

	/**
	 * 캔들 시간 계산 (타임프레임 단위로 내림)
	 */
	private Long calculateCandleTime(final Long timeInSeconds, final Long timeFrameSeconds) {
		return timeInSeconds - (timeInSeconds % timeFrameSeconds);
	}

	/**
	 * 거래 내역 메모리 저장 및 캔들 업데이트
	 */
	public void processNewTrade(final TradeHistory tradeHistory) {
		if (tradeHistory == null || tradeHistory.getCompanyCode() == null) {
			return;
		}

		final String companyCode = tradeHistory.getCompanyCode();
		storeTradeHistory(companyCode, tradeHistory);
		updateCandlesWithTrade(companyCode, tradeHistory);
		sendChartUpdates(companyCode, tradeHistory);
	}

	/**
	 * 거래 내역 메모리 저장
	 */
	private void storeTradeHistory(final String companyCode, final TradeHistory tradeHistory) {
		final ConcurrentLinkedQueue<TradeHistory> trades = recentTradesMap.computeIfAbsent(
				companyCode, k -> new ConcurrentLinkedQueue<>());

		trades.offer(tradeHistory);

		// 최대 개수 유지
		while (trades.size() > MAX_TRADE_HISTORY) {
			trades.poll();
		}
	}

	/**
	 * 거래 내역으로 캔들 업데이트
	 */
	private void updateCandlesWithTrade(final String companyCode, final TradeHistory tradeHistory) {
		final ReentrantReadWriteLock lock = companyLocks.computeIfAbsent(
				companyCode, k -> new ReentrantReadWriteLock());

		lock.writeLock().lock();
		try {
			final Map<TimeFrame, List<CandleDto>> companyTimeFrameMap =
					timeFrameCandleMap.computeIfAbsent(companyCode, k -> new EnumMap<>(TimeFrame.class));

			for (TimeFrame timeFrame : TimeFrame.values()) {
				updateTimeFrameCandle(tradeHistory, companyTimeFrameMap, timeFrame);
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * 특정 타임프레임 캔들 업데이트
	 */
	private void updateTimeFrameCandle(
			final TradeHistory tradeHistory,
			final Map<TimeFrame, List<CandleDto>> companyTimeFrameMap,
			final TimeFrame timeFrame) {

		final List<CandleDto> candles = companyTimeFrameMap.computeIfAbsent(
				timeFrame, k -> new ArrayList<>());

		final Long tradeTime = tradeHistory.getTradeTime();
		final Long timeFrameSeconds = timeFrame.getSeconds();
		final Long candleTime = calculateCandleTime(tradeTime, timeFrameSeconds);

		final Double price = tradeHistory.getPrice().doubleValue();
		final Integer volume = tradeHistory.getQuantity().intValue();

		if (candles.isEmpty()) {
			// 첫 캔들 생성
			candles.add(createCandle(candleTime, price, price, price, price, volume));
			return;
		}

		final CandleDto lastCandle = candles.get(candles.size() - 1);

		if (lastCandle.time().equals(candleTime)) {
			// 현재 캔들 업데이트
			final CandleDto updatedCandle = CandleDto.builder()
					.time(lastCandle.time())
					.open(lastCandle.open())
					.high(Math.max(lastCandle.high(), price))
					.low(Math.min(lastCandle.low(), price))
					.close(price)
					.volume(lastCandle.volume() + volume)
					.build();

			candles.set(candles.size() - 1, updatedCandle);
		} else if (candleTime > lastCandle.time()) {
			// 새 캔들 추가
			for (Long time = lastCandle.time() + timeFrameSeconds; time < candleTime; time += timeFrameSeconds) {
				candles.add(createCandle(time, lastCandle.close(), lastCandle.close(),
						lastCandle.close(), lastCandle.close(), 0));
			}

			candles.add(createCandle(candleTime, price, price, price, price, volume));

			// 캔들 개수 제한
			if (candles.size() > CANDLE_KEEP_NUMBER) {
				List<CandleDto> limitedCandles = new ArrayList<>(
						candles.subList(candles.size() - CANDLE_KEEP_NUMBER, candles.size()));
				candles.clear();
				candles.addAll(limitedCandles);
			}
		}
	}

	/**
	 * 차트 업데이트 전송
	 */
	private void sendChartUpdates(final String companyCode, final TradeHistory tradeHistory) {
		final Double price = tradeHistory.getPrice().doubleValue();
		final Integer volume = tradeHistory.getQuantity().intValue();

		// 기본 업데이트 전송
		final ChartUpdateDto basicUpdateDto = ChartUpdateDto.builder()
				.price(price)
				.volume(volume)
				.build();

		messagingTemplate.convertAndSend(String.format(CHART_TOPIC_FORMAT, companyCode), basicUpdateDto);

		// 타임프레임별 업데이트 전송
		final ReentrantReadWriteLock lock = companyLocks.get(companyCode);
		if (lock == null)
			return;

		lock.readLock().lock();
		try {
			final Map<TimeFrame, List<CandleDto>> companyTimeFrameMap = timeFrameCandleMap.get(companyCode);
			if (companyTimeFrameMap == null)
				return;

			for (TimeFrame timeFrame : TimeFrame.values()) {
				final List<CandleDto> candles = companyTimeFrameMap.get(timeFrame);
				if (candles == null || candles.isEmpty())
					continue;

				final CandleDto latestCandle = candles.get(candles.size() - 1);

				final ChartUpdateDto timeFrameUpdateDto = ChartUpdateDto.builder()
						.price(latestCandle.close())
						.volume(latestCandle.volume())
						.timeCode(timeFrame.getTimeCode())
						.build();

				final String destination = String.format(
						TIMEFRAME_CHART_TOPIC_FORMAT, companyCode, timeFrame.getTimeCode());
				messagingTemplate.convertAndSend(destination, timeFrameUpdateDto);
			}
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * 차트 기록 조회
	 */
	public ChartResponseDto getChartHistory(final String companyCode, final String timeframeCode) {
		if (companyCode == null || companyCode.isEmpty() ||
				timeframeCode == null || timeframeCode.isEmpty()) {
			return createEmptyChartResponse(TimeFrame.MINUTE_15.getTimeCode());
		}

		TimeFrame requestedTimeFrame = TimeFrame.MINUTE_15; // 기본값

		// 타임프레임 코드 검색
		for (TimeFrame tf : TimeFrame.values()) {
			if (tf.getTimeCode().equals(timeframeCode)) {
				requestedTimeFrame = tf;
				break;
			}
		}

		// 현재 시간까지의 캔들 업데이트
		updateCurrentCandles(companyCode, requestedTimeFrame);

		final ReentrantReadWriteLock lock = companyLocks.computeIfAbsent(
				companyCode, k -> new ReentrantReadWriteLock());

		lock.readLock().lock();
		try {
			final Map<TimeFrame, List<CandleDto>> companyTimeFrameMap = timeFrameCandleMap.get(companyCode);
			if (companyTimeFrameMap == null) {
				return createEmptyChartResponse(requestedTimeFrame.getTimeCode());
			}

			final List<CandleDto> timeFrameCandles = companyTimeFrameMap.get(requestedTimeFrame);
			if (timeFrameCandles == null || timeFrameCandles.isEmpty()) {
				return createEmptyChartResponse(requestedTimeFrame.getTimeCode());
			}

			return ChartResponseDto.builder()
					.candles(new ArrayList<>(timeFrameCandles))
					.timeCode(requestedTimeFrame.getTimeCode())
					.build();
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * 빈 차트 응답 생성
	 */
	private ChartResponseDto createEmptyChartResponse(final String timeCode) {
		return ChartResponseDto.builder()
				.candles(Collections.emptyList())
				.timeCode(timeCode)
				.build();
	}

	/**
	 * 마지막 거래 조회
	 */
	public Optional<TradeHistory> getLastTrade(final String companyCode) {
		if (companyCode == null || companyCode.trim().isEmpty()) {
			return Optional.empty();
		}

		final ConcurrentLinkedQueue<TradeHistory> trades = recentTradesMap.get(companyCode);
		return trades == null || trades.isEmpty() ?
				Optional.empty() : Optional.of(trades.peek());
	}
}
