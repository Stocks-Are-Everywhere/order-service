package com.onseju.orderservice.chart.controller;

import java.util.Set;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import com.onseju.orderservice.chart.domain.TimeFrame;
import com.onseju.orderservice.chart.dto.ChartResponseDto;
import com.onseju.orderservice.chart.service.ChartService;
import com.onseju.orderservice.company.service.ClosingPriceService;
import com.onseju.orderservice.events.listener.ApplicationReadyEventListener;

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
	private final ApplicationReadyEventListener applicationReadyEventListener;
	private final ClosingPriceService closingPriceService;

	private static final String TOPIC_TEMPLATE = "/topic/candle/%s/%s";

	/**
	 * 15초봉 업데이트 (15초마다)
	 */
	@Scheduled(fixedRate = 15000)
	public void sendCandleUpdates15Sec() {
		updateAllCompanies(TimeFrame.SECONDS_15);
	}

	/**
	 * 1분봉 업데이트 (60초마다)
	 */
	@Scheduled(fixedRate = 60000)
	public void sendCandleUpdates1Min() {
		updateAllCompanies(TimeFrame.MINUTE_1);
	}

	/**
	 * 5분봉 업데이트 (5분마다)
	 */
	@Scheduled(fixedRate = 300000)
	public void sendCandleUpdates5Min() {
		updateAllCompanies(TimeFrame.MINUTE_5);
	}

	/**
	 * 15분봉 업데이트 (15분마다)
	 */
	@Scheduled(fixedRate = 900000)
	public void sendCandleUpdates15Min() {
		updateAllCompanies(TimeFrame.MINUTE_15);
	}

	/**
	 * 30분봉 업데이트 (30분마다)
	 */
	@Scheduled(fixedRate = 1800000)
	public void sendCandleUpdates30Min() {
		updateAllCompanies(TimeFrame.MINUTE_30);
	}

	/**
	 * 1시간봉 업데이트 (1시간마다)
	 */
	@Scheduled(fixedRate = 3600000)
	public void sendCandleUpdates1Hour() {
		updateAllCompanies(TimeFrame.HOUR_1);
	}

	/**
	 * 모든 종목에 대해 지정된 타임프레임의 캔들 업데이트 및 전송
	 */
	private void updateAllCompanies(final TimeFrame timeFrame) {
		if (!applicationReadyEventListener.isInitialized()) {
			log.warn("애플리케이션이 아직 초기화되지 않았습니다. 차트 스케줄러를 건너뜁니다.");
			return;
		}

		try {
			// 모든 종목 코드 가져오기
			final Set<String> allCompanyCodes = closingPriceService.getAllCompanyCodeByInmemory();

			if (allCompanyCodes.isEmpty()) {
				log.warn("업데이트할 종목이 없습니다.");
				return;
			}

			log.debug("{}분봉 업데이트 시작: {} 종목", timeFrame.getTimeCode(), allCompanyCodes.size());

			// 모든 종목에 대해 업데이트 및 전송
			for (String companyCode : allCompanyCodes) {
				sendCandleUpdates(companyCode, timeFrame);
			}

			log.debug("{}분봉 업데이트 완료", timeFrame.getTimeCode());
		} catch (Exception e) {
			log.error("{}분봉 전체 종목 업데이트 중 오류 발생", timeFrame.getTimeCode(), e);
		}
	}

	/**
	 * 지정된 종목 코드와 타임프레임에 대한 캔들 업데이트 수행 및 전송
	 */
	private void sendCandleUpdates(final String companyCode, final TimeFrame timeFrame) {
		try {
			// 종목의 캔들 데이터 업데이트
			chartService.updateCandles(companyCode);

			// 업데이트된 캔들 데이터 조회 및 전송
			final ChartResponseDto candleData = chartService.getChartHistory(companyCode, timeFrame.getTimeCode());
			final String destination = String.format(TOPIC_TEMPLATE, companyCode, timeFrame.getTimeCode());
			messagingTemplate.convertAndSend(destination, candleData);
		} catch (Exception e) {
			log.error("캔들 업데이트 중 오류 발생: {}분봉, 종목코드={}", timeFrame.getTimeCode(), companyCode, e);
		}
	}
}
