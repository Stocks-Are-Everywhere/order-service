package com.onseju.orderservice.tradehistory.service;

import com.onseju.orderservice.tradehistory.domain.TradeHistory;
import com.onseju.orderservice.tradehistory.dto.TotalTradeAmountDto;
import com.onseju.orderservice.tradehistory.dto.TradeAvgPriceDto;
import com.onseju.orderservice.tradehistory.dto.TradeCountDto;
import com.onseju.orderservice.tradehistory.service.repository.TradeHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TradeRankingServiceTest {

	@Mock
	private TradeHistoryRepository tradeHistoryRepository;

	private TradeRankingService tradeRankingService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		tradeRankingService = new TradeRankingService(tradeHistoryRepository);

		// 테스트 데이터 설정
		setupMockData();
	}

	private void setupMockData() {
		// 총 거래액 데이터 설정
		List<Object[]> totalAmountData = new ArrayList<>();
		totalAmountData.add(new Object[]{"COMP5", new BigDecimal("2500.00")});
		totalAmountData.add(new Object[]{"COMP4", new BigDecimal("1600.00")});
		totalAmountData.add(new Object[]{"COMP3", new BigDecimal("900.00")});

		// 평균 가격 데이터 설정
		List<Object[]> avgPriceData = new ArrayList<>();
		avgPriceData.add(new Object[]{"COMP5", new BigDecimal("250.00")});
		avgPriceData.add(new Object[]{"COMP4", new BigDecimal("200.00")});
		avgPriceData.add(new Object[]{"COMP3", new BigDecimal("150.00")});

		// 거래 건수 데이터 설정
		List<Object[]> countData = new ArrayList<>();
		countData.add(new Object[]{"COMP5", 10L});
		countData.add(new Object[]{"COMP4", 8L});
		countData.add(new Object[]{"COMP3", 6L});

		// Mock 설정
		when(tradeHistoryRepository.findTotalTradeAmountByCompany(any(Pageable.class)))
			.thenReturn(totalAmountData);
		when(tradeHistoryRepository.findTradeAvgPriceByCompany(any(Pageable.class)))
			.thenReturn(avgPriceData);
		when(tradeHistoryRepository.findTradeCountByCompany(any(Pageable.class)))
			.thenReturn(countData);
	}

	@Test
	@DisplayName("캐시 갱신 후 총 거래액 랭킹 조회")
	void getTotalTradeAmounts_AfterRefresh_ReturnsCorrectRanking() {
		// given
		tradeRankingService.refreshCache();

		// when
		List<TotalTradeAmountDto> result = tradeRankingService.getTotalTradeAmounts();

		// then
		assertThat(result).isNotEmpty();
		assertThat(result.size()).isEqualTo(3);
		assertThat(result.get(0).companyCode()).isEqualTo("COMP5");
		assertThat(result.get(0).totalAmount()).isEqualByComparingTo(new BigDecimal("2500.00"));

		// 리포지토리 메서드 호출 확인
		verify(tradeHistoryRepository, times(1)).findTotalTradeAmountByCompany(any(Pageable.class));
	}

	@Test
	@DisplayName("캐시 갱신 후 평균 가격 랭킹 조회")
	void getTradeAvgPrices_AfterRefresh_ReturnsCorrectRanking() {
		// given
		tradeRankingService.refreshCache();

		// when
		List<TradeAvgPriceDto> result = tradeRankingService.getTradeAvgPrices();

		// then
		assertThat(result).isNotEmpty();
		assertThat(result.size()).isEqualTo(3);
		assertThat(result.get(0).companyCode()).isEqualTo("COMP5");
		assertThat(result.get(0).avgPrice()).isEqualByComparingTo(new BigDecimal("250.00"));

		// 리포지토리 메서드 호출 확인
		verify(tradeHistoryRepository, times(1)).findTradeAvgPriceByCompany(any(Pageable.class));
	}

	@Test
	@DisplayName("캐시 갱신 후 거래 건수 랭킹 조회")
	void getTradeCounts_AfterRefresh_ReturnsCorrectRanking() {
		// given
		tradeRankingService.refreshCache();

		// when
		List<TradeCountDto> result = tradeRankingService.getTradeCounts();

		// then
		assertThat(result).isNotEmpty();
		assertThat(result.size()).isEqualTo(3);
		assertThat(result.get(0).companyCode()).isEqualTo("COMP5");
		assertThat(result.get(0).count()).isEqualTo(10L);

		// 리포지토리 메서드 호출 확인
		verify(tradeHistoryRepository, times(1)).findTradeCountByCompany(any(Pageable.class));
	}

	@Test
	@DisplayName("캐시 갱신 전 빈 결과 반환")
	void getResults_BeforeRefresh_ReturnsEmptyLists() {
		// when
		List<TotalTradeAmountDto> totalAmounts = tradeRankingService.getTotalTradeAmounts();
		List<TradeAvgPriceDto> avgPrices = tradeRankingService.getTradeAvgPrices();
		List<TradeCountDto> tradeCounts = tradeRankingService.getTradeCounts();

		// then
		assertThat(totalAmounts).isEmpty();
		assertThat(avgPrices).isEmpty();
		assertThat(tradeCounts).isEmpty();

		// 리포지토리 메서드 호출 확인 (호출되지 않아야 함)
		verify(tradeHistoryRepository, never()).findTotalTradeAmountByCompany(any(Pageable.class));
		verify(tradeHistoryRepository, never()).findTradeAvgPriceByCompany(any(Pageable.class));
		verify(tradeHistoryRepository, never()).findTradeCountByCompany(any(Pageable.class));
	}

	@Test
	@DisplayName("예외 발생 시 캐시가 비어있는 상태 유지")
	void refreshCache_WhenExceptionOccurs_MaintainsEmptyCache() {
		// given
		when(tradeHistoryRepository.findTotalTradeAmountByCompany(any(Pageable.class)))
			.thenThrow(new RuntimeException("Database error"));

		// when
		tradeRankingService.refreshCache(); // 예외가 발생하지만 내부적으로 처리됨

		// then
		assertThat(tradeRankingService.getTotalTradeAmounts()).isEmpty();
		assertThat(tradeRankingService.getTradeAvgPrices()).isEmpty();
		assertThat(tradeRankingService.getTradeCounts()).isEmpty();

		// 리포지토리 메서드 호출 확인
		verify(tradeHistoryRepository, times(1)).findTotalTradeAmountByCompany(any(Pageable.class));
	}

	@Test
	@DisplayName("Double 타입 변환 테스트")
	void refreshCache_WithDoubleValues_ConvertsCorrectly() {
		// given
		List<Object[]> totalAmountData = new ArrayList<>();
		totalAmountData.add(new Object[]{"COMP1", 1000.0});

		List<Object[]> avgPriceData = new ArrayList<>();
		avgPriceData.add(new Object[]{"COMP1", 100.0});

		when(tradeHistoryRepository.findTotalTradeAmountByCompany(any(Pageable.class)))
			.thenReturn(totalAmountData);
		when(tradeHistoryRepository.findTradeAvgPriceByCompany(any(Pageable.class)))
			.thenReturn(avgPriceData);

		// when
		tradeRankingService.refreshCache();

		// then
		List<TotalTradeAmountDto> totalAmounts = tradeRankingService.getTotalTradeAmounts();
		List<TradeAvgPriceDto> avgPrices = tradeRankingService.getTradeAvgPrices();

		assertThat(totalAmounts).hasSize(1);
		assertThat(totalAmounts.get(0).totalAmount()).isEqualByComparingTo(new BigDecimal("1000.0"));

		assertThat(avgPrices).hasSize(1);
		assertThat(avgPrices.get(0).avgPrice()).isEqualByComparingTo(new BigDecimal("100.0"));
	}
}
