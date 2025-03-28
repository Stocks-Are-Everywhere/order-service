package com.onseju.orderservice.chart.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
			// 데이터 조회
			final ChartResponseDto chartData = chartService.getChartHistory(symbol, timeFrame);

			// 응답 유효성 검증
			if (chartData == null || chartData.candles() == null || chartData.candles().isEmpty()) {
				log.warn("차트 데이터가 비어있습니다: 종목={}, 타임프레임={}", symbol, timeFrame);
				return ResponseEntity.noContent().build();
			}

			// 정상 응답
			return ResponseEntity.ok(chartData);
		} catch (Exception e) {
			log.error("차트 히스토리 조회 중 오류 발생: 종목={}, 타임프레임={}, 오류={}",
					symbol, timeFrame, e.getMessage(), e);
			return ResponseEntity.badRequest().build();
		}
	}
}
