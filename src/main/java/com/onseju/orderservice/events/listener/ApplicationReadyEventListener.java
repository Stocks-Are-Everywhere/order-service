package com.onseju.orderservice.events.listener;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.onseju.orderservice.chart.service.ChartService;
import com.onseju.orderservice.company.service.CompanyService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationReadyEventListener {
	private final ChartService chartService;
	private final CompanyService companyService;

	@EventListener(ApplicationReadyEvent.class)
	public void initialize() {
		// DB에서 거래 내역 로드
		chartService.loadTradeHistoryFromDb();

		// 종목별 초기 종가 설정
		companyService.refreshClosingPrices();

		// 거래 내역과 종가 기반 캔들 업데이트
		chartService.initializeCandlesFromTrades();
	}
}
