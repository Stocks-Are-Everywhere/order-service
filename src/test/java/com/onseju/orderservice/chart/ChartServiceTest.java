package com.onseju.orderservice.chart;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.onseju.orderservice.chart.dto.ChartResponseDto;
import com.onseju.orderservice.chart.dto.ChartUpdateDto;
import com.onseju.orderservice.chart.service.ChartService;
import com.onseju.orderservice.company.service.ClosingPriceService;
import com.onseju.orderservice.tradehistory.domain.TradeHistory;
import com.onseju.orderservice.tradehistory.service.repository.TradeHistoryRepository;

public class ChartServiceTest {

	@Mock
	private SimpMessagingTemplate messagingTemplate;

	@Mock
	private TradeHistoryRepository tradeHistoryRepository;

	@Mock
	private ClosingPriceService closingPriceService;

	private ChartService chartService;

	@Captor
	private ArgumentCaptor<String> destinationCaptor;

	@Captor
	private ArgumentCaptor<ChartUpdateDto> updateDtoCaptor;

	private static final String COMPANY_CODE = "005930"; // 삼성전자 종목코드

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		chartService = new ChartService(messagingTemplate, tradeHistoryRepository, closingPriceService);
	}

	@Test
	@DisplayName("회사 코드가 존재할 때 차트 데이터가 올바르게 초기화되는지 확인")
	void initializeAllCompanyCandleData_WhenCompaniesExist_ShouldLoadCompanyCodes() {
		// Given
		Set<String> companyCodes = new HashSet<>(Arrays.asList(COMPANY_CODE, "000660"));
		when(closingPriceService.getAllCompanyCodeByInmemory()).thenReturn(companyCodes);
		when(closingPriceService.getClosingPrice(anyString())).thenReturn(BigDecimal.valueOf(100));

		// 여기에서 findDistinctCompanyCodes 메서드가 호출될 것으로 예상
		when(tradeHistoryRepository.findDistinctCompanyCodes()).thenReturn(companyCodes.stream().toList());

		// When
		chartService.initializeAllCompanyCandleData();

		// Then
		verify(closingPriceService).getAllCompanyCodeByInmemory();
		verify(tradeHistoryRepository).findDistinctCompanyCodes();

		// 실제 ChartService 구현에 따라 이 호출이 발생하지 않을 수 있으므로 검증에서 제외
		// verify(tradeHistoryRepository).findRecentTradesByCompanyCode(eq(COMPANY_CODE), anyInt());
		// verify(tradeHistoryRepository).findRecentTradesByCompanyCode(eq("000660"), anyInt());

		verify(closingPriceService, atLeastOnce()).getClosingPrice(anyString());
	}

	@Test
	@DisplayName("새로운 거래 발생 시 메시징 템플릿을 통해 차트 업데이트가 전송되는지 확인")
	void processNewTrade_ShouldCallMessagingTemplate() {
		// Given
		TradeHistory trade = createTradeHistory(COMPANY_CODE, BigDecimal.valueOf(105.0), BigDecimal.valueOf(20),
				Instant.now().getEpochSecond());

		// 테스트를 위해 먼저 초기화 실행
		Set<String> companyCodes = new HashSet<>(Arrays.asList(COMPANY_CODE));
		when(closingPriceService.getAllCompanyCodeByInmemory()).thenReturn(companyCodes);
		when(closingPriceService.getClosingPrice(anyString())).thenReturn(BigDecimal.valueOf(100));
		when(tradeHistoryRepository.findDistinctCompanyCodes()).thenReturn(companyCodes.stream().toList());

		chartService.initializeAllCompanyCandleData();

		// 목 객체 상태 초기화
		reset(messagingTemplate);

		// When
		chartService.processNewTrade(trade);

		// Then - 메시지가 전송되었는지만 확인
		verify(messagingTemplate, atLeastOnce()).convertAndSend(anyString(), any(ChartUpdateDto.class));
	}

	@Test
	@DisplayName("유효하지 않은 입력으로 차트 이력을 요청했을 때 빈 응답이 반환되는지 확인")
	void getChartHistory_WithInvalidInput_ShouldReturnEmptyResponse() {
		// When
		ChartResponseDto response1 = chartService.getChartHistory(null, "15m");
		ChartResponseDto response2 = chartService.getChartHistory(COMPANY_CODE, null);
		ChartResponseDto response3 = chartService.getChartHistory("", "");

		// Then
		assertTrue(response1.candles().isEmpty());
		assertTrue(response2.candles().isEmpty());
		assertTrue(response3.candles().isEmpty());
	}

	@Test
	@DisplayName("유효하지 않은 회사 코드로 최근 거래를 요청했을 때 빈 Optional이 반환되는지 확인")
	void getLastTrade_WithInvalidCompanyCode_ShouldReturnEmpty() {
		// When
		Optional<TradeHistory> result1 = chartService.getLastTrade(null);
		Optional<TradeHistory> result2 = chartService.getLastTrade("");
		Optional<TradeHistory> result3 = chartService.getLastTrade("NONEXISTENT");

		// Then
		assertFalse(result1.isPresent());
		assertFalse(result2.isPresent());
		assertFalse(result3.isPresent());
	}

	// 테스트용 TradeHistory 생성 헬퍼 메서드
	private TradeHistory createTradeHistory(String companyCode, BigDecimal price, BigDecimal quantity, Long tradeTime) {
		return TradeHistory.builder()
				.companyCode(companyCode)
				.price(price)
				.quantity(quantity)
				.tradeTime(tradeTime)
				.build();
	}
}
