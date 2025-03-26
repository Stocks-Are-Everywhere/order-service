package com.onseju.orderservice.chart.controller;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import com.onseju.orderservice.chart.domain.TimeFrame;
import com.onseju.orderservice.chart.dto.ChartResponseDto;
import com.onseju.orderservice.chart.service.ChartService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Tag(name = "차트 WebSocket API", description = "초기 데이터를 기반으로 각 타임프레임별 새로운 캔들을 전달하는 컨드롤러 입니다.")
@Slf4j
public class ChartScheduler {

	private final ChartService chartService;
	private final SimpMessagingTemplate messagingTemplate;

	// 기본 종목 코드
	private static final String DEFAULT_COMPANY_CODE = "005930";
	private static final String TOPIC_TEMPLATE = "/topic/candle/%s/%s";

	/**
	 * 15초봉 업데이트 (15초마다)
	 */
	@Scheduled(fixedRate = 15000)
	public void sendCandleUpdates15Sec() {
		sendCandleUpdates(DEFAULT_COMPANY_CODE, TimeFrame.SECONDS_15);
	}

	/**
	 * 1분봉 업데이트 (60초마다)
	 */
	@Scheduled(fixedRate = 60000)
	public void sendCandleUpdates1Min() {
		sendCandleUpdates(DEFAULT_COMPANY_CODE, TimeFrame.MINUTE_1);
	}

	/**
	 * 5분봉 업데이트 (5분마다)
	 */
	@Scheduled(fixedRate = 300000)
	public void sendCandleUpdates5Min() {
		sendCandleUpdates(DEFAULT_COMPANY_CODE, TimeFrame.MINUTE_5);
	}

	/**
	 * 15분봉 업데이트 (15분마다)
	 */
	@Scheduled(fixedRate = 900000)
	public void sendCandleUpdates15Min() {
		sendCandleUpdates(DEFAULT_COMPANY_CODE, TimeFrame.MINUTE_15);
	}

	/**
	 * 30분봉 업데이트 (30분마다)
	 */
	@Scheduled(fixedRate = 1800000)
	public void sendCandleUpdates30Min() {
		sendCandleUpdates(DEFAULT_COMPANY_CODE, TimeFrame.MINUTE_30);
	}

	/**
	 * 1시간봉 업데이트 (1시간마다)
	 */
	@Scheduled(fixedRate = 3600000)
	public void sendCandleUpdates1Hour() {
		sendCandleUpdates(DEFAULT_COMPANY_CODE, TimeFrame.HOUR_1);
	}

	/**
	 * 지정된 종목 코드와 타임프레임에 대한 캔들 업데이트 수행 및 전송
	 */
	private void sendCandleUpdates(final String companyCode, final TimeFrame timeFrame) {
		// 캔들 데이터 업데이트
		chartService.updateCandles(companyCode);

		// 업데이트된 캔들 데이터 조회 및 전송
		final ChartResponseDto candleDate = chartService.getChartHistory(companyCode, timeFrame.getTimeCode());
		final String destination = String.format(TOPIC_TEMPLATE, companyCode, timeFrame.getTimeCode());
		messagingTemplate.convertAndSend(destination, candleDate);
	}

}
