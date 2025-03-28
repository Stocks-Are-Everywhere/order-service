package com.onseju.orderservice.events.listener;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
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

	// 초기화 상태 추적을 위한 플래그
	private final AtomicBoolean closingPricesInitialized = new AtomicBoolean(false);
	private final AtomicBoolean chartDataInitialized = new AtomicBoolean(false);

	@EventListener(ApplicationReadyEvent.class)
	@Order(1)
	public void initializeClosingPrices() {
		// 종목별 초기 종가 설정
		companyService.refreshClosingPrices();
		closingPricesInitialized.set(true);
	}

	@EventListener(ApplicationReadyEvent.class)
	@Order(2)
	public void initializeChartData() {
		// 모든 종목 캔들 데이터 초기화
		chartService.initializeAllCompanyCandleData();
		chartDataInitialized.set(true);
	}

	/**
	 * 차트 스케줄러에서 호출할 초기화 상태 확인 메서드
	 */
	public boolean isInitialized() {
		return closingPricesInitialized.get() && chartDataInitialized.get();
	}
}
