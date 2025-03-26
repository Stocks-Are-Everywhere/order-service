package com.onseju.orderservice.chart.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.onseju.orderservice.chart.dto.CandleDto;
import com.onseju.orderservice.chart.dto.ChartResponseDto;
import com.onseju.orderservice.chart.service.ChartService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/chart")
@Tag(name = "차트 초기 데이터 API", description = "차트 생성시 초기 데이터를 보내주는 컨트롤러 입니다.")
@RequiredArgsConstructor
@Slf4j
public class ChartRestController {

	private final ChartService chartService;

	@GetMapping("/{symbol}/history")
	@Operation(summary = "차트 히스토리 조회", description = "특정 종목의 차트 데이터를 조회합니다.")
	public ResponseEntity<ChartResponseDto> getChartHistory(
			@PathVariable("symbol")
			@Parameter(description = "종목 코드", required = true)
			String symbol,

			@RequestParam(value = "timeFrame", defaultValue = "15m")
			@Parameter(description = "타임프레임 (15s, 1m, 5m, 15m, 30m, 1h)", example = "15m")
			String timeFrame
	) {
		try {
			return processChartHistoryRequest(symbol, timeFrame);
		} catch (Exception e) {
			log.error("차트 히스토리 조회 중 오류 발생: 종목={}, 타임프레임={}, 오류={}",
					symbol, timeFrame, e.getMessage(), e);
			return ResponseEntity.badRequest().build();
		}
	}

	/**
	 * 차트 히스토리 요청 처리
	 */
	private ResponseEntity<ChartResponseDto> processChartHistoryRequest(final String symbol, final String timeFrame) {
		// 데이터 조회
		final ChartResponseDto chartData = chartService.getChartHistory(symbol, timeFrame);

		// 응답 유효성 검증
		if (chartData == null || chartData.candles() == null) {
			log.warn("차트 데이터가 null입니다: 종목={}, 타임프레임={}", symbol, timeFrame);
			return ResponseEntity.noContent().build();
		}

		// 캔들 데이터 유효성 검증
		if (containsInvalidCandles(chartData.candles())) {
			return handleInvalidCandles(symbol, timeFrame, chartData);
		}

		// 정상 응답
		return ResponseEntity.ok(chartData);
	}

	/**
	 * 유효하지 않은 캔들 포함 여부 확인
	 */
	private Boolean containsInvalidCandles(final List<CandleDto> candles) {
		return candles.stream()
				.anyMatch(this::isInvalidCandle);
	}

	/**
	 * 개별 캔들 유효성 검사
	 */
	private Boolean isInvalidCandle(final CandleDto candle) {
		return candle == null || candle.time() == null || candle.time() <= 0;
	}

	/**
	 * 유효하지 않은 캔들 처리
	 */
	private ResponseEntity<ChartResponseDto> handleInvalidCandles(
			final String symbol,
			final String timeFrame,
			final ChartResponseDto chartData
	) {
		log.warn("유효하지 않은 캔들이 포함되어 있습니다: 종목={}, 타임프레임={}", symbol, timeFrame);

		// 유효한 캔들만 필터링한 응답 생성
		final ChartResponseDto filteredData = createFilteredChartResponse(chartData);
		return ResponseEntity.ok(filteredData);
	}

	/**
	 * 필터링된 차트 응답 생성
	 */
	private ChartResponseDto createFilteredChartResponse(final ChartResponseDto chartData) {
		return ChartResponseDto.builder()
				.candles(chartData.candles().stream()
						.filter(candle -> !isInvalidCandle(candle))
						.toList())
				.timeCode(chartData.timeCode())
				.build();
	}
}
