package com.onseju.orderservice.tradehistory.service;

import com.onseju.orderservice.company.domain.Company;
import com.onseju.orderservice.company.exception.CompanyNotFound;
import com.onseju.orderservice.company.service.repository.CompanyRepository;
import com.onseju.orderservice.tradehistory.dto.TotalTradeAmountDto;
import com.onseju.orderservice.tradehistory.dto.TradeAvgPriceDto;
import com.onseju.orderservice.tradehistory.dto.TradeCountDto;
import com.onseju.orderservice.tradehistory.service.repository.TradeHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class TradeRankingServiceTest {

	@Mock
	private TradeHistoryRepository tradeHistoryRepository;

	@Mock
	private CompanyRepository companyRepository;

	private TradeRankingService tradeRankingService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		tradeRankingService = new TradeRankingService(tradeHistoryRepository, companyRepository);

		setupMockData();
	}

	private void setupMockData() {
		// 테스트 데이터 설정
		List<Object[]> totalAmountData = new ArrayList<>();
		totalAmountData.add(new Object[]{"COMP5", new BigDecimal("2500.00")});
		totalAmountData.add(new Object[]{"COMP4", new BigDecimal("1600.00")});
		totalAmountData.add(new Object[]{"COMP3", new BigDecimal("900.00")});

		List<Object[]> avgPriceData = new ArrayList<>();
		avgPriceData.add(new Object[]{"COMP5", new BigDecimal("250.00")});
		avgPriceData.add(new Object[]{"COMP4", new BigDecimal("200.00")});
		avgPriceData.add(new Object[]{"COMP3", new BigDecimal("150.00")});

		List<Object[]> countData = new ArrayList<>();
		countData.add(new Object[]{"COMP5", 10L});
		countData.add(new Object[]{"COMP4", 8L});
		countData.add(new Object[]{"COMP3", 6L});

		// 리포지토리 스터빙
		doReturn(totalAmountData).when(tradeHistoryRepository).findTotalTradeAmountByCompany(any(Pageable.class));
		doReturn(avgPriceData).when(tradeHistoryRepository).findTradeAvgPriceByCompany(any(Pageable.class));
		doReturn(countData).when(tradeHistoryRepository).findTradeCountByCompany(any(Pageable.class));

		// 회사 객체 스터빙
		Company company5 = mock(Company.class);
		doReturn("회사5").when(company5).getIsuNm();
		doReturn(company5).when(companyRepository).findByIsuSrtCd("COMP5");

		Company company4 = mock(Company.class);
		doReturn("회사4").when(company4).getIsuNm();
		doReturn(company4).when(companyRepository).findByIsuSrtCd("COMP4");

		Company company3 = mock(Company.class);
		doReturn("회사3").when(company3).getIsuNm();
		doReturn(company3).when(companyRepository).findByIsuSrtCd("COMP3");
	}

	@Test
	@DisplayName("캐시 갱신 후 총 거래액 랭킹 조회")
	void getTotalTradeAmounts_AfterRefresh_ReturnsCorrectRanking() {
		tradeRankingService.refreshCache();

		List<TotalTradeAmountDto> result = tradeRankingService.getTotalTradeAmounts();

		assertThat(result).hasSize(3);
		assertThat(result.get(0).companyCode()).isEqualTo("COMP5");
		assertThat(result.get(0).companyName()).isEqualTo("회사5");
		assertThat(result.get(0).totalAmount()).isEqualByComparingTo(new BigDecimal("2500.00"));

		verify(tradeHistoryRepository).findTotalTradeAmountByCompany(any(Pageable.class));
		verify(companyRepository, atLeastOnce()).findByIsuSrtCd("COMP5");
	}

	@Test
	@DisplayName("캐시 갱신 후 평균 가격 랭킹 조회")
	void getTradeAvgPrices_AfterRefresh_ReturnsCorrectRanking() {
		tradeRankingService.refreshCache();

		List<TradeAvgPriceDto> result = tradeRankingService.getTradeAvgPrices();

		assertThat(result).hasSize(3);
		assertThat(result.get(0).companyCode()).isEqualTo("COMP5");
		assertThat(result.get(0).companyName()).isEqualTo("회사5");
		assertThat(result.get(0).avgPrice()).isEqualByComparingTo(new BigDecimal("250.00"));

		verify(tradeHistoryRepository).findTradeAvgPriceByCompany(any(Pageable.class));
		verify(companyRepository, atLeastOnce()).findByIsuSrtCd("COMP5");
	}

	@Test
	@DisplayName("캐시 갱신 후 거래 건수 랭킹 조회")
	void getTradeCounts_AfterRefresh_ReturnsCorrectRanking() {
		tradeRankingService.refreshCache();

		List<TradeCountDto> result = tradeRankingService.getTradeCounts();

		assertThat(result).hasSize(3);
		assertThat(result.get(0).companyCode()).isEqualTo("COMP5");
		assertThat(result.get(0).companyName()).isEqualTo("회사5");
		assertThat(result.get(0).count()).isEqualTo(10L);

		verify(tradeHistoryRepository).findTradeCountByCompany(any(Pageable.class));
		verify(companyRepository, atLeastOnce()).findByIsuSrtCd("COMP5");
	}

	@Test
	@DisplayName("캐시 갱신 전 빈 결과 반환")
	void getResults_BeforeRefresh_ReturnsEmptyLists() {
		assertThat(tradeRankingService.getTotalTradeAmounts()).isEmpty();
		assertThat(tradeRankingService.getTradeAvgPrices()).isEmpty();
		assertThat(tradeRankingService.getTradeCounts()).isEmpty();

		verify(tradeHistoryRepository, never()).findTotalTradeAmountByCompany(any(Pageable.class));
		verify(companyRepository, never()).findByIsuSrtCd(anyString());
	}

	@Test
	@DisplayName("예외 발생 시 캐시가 비어있는 상태 유지")
	void refreshCache_WhenExceptionOccurs_MaintainsEmptyCache() {
		doThrow(new RuntimeException("Database error")).when(tradeHistoryRepository).findTotalTradeAmountByCompany(any(Pageable.class));

		tradeRankingService.refreshCache();

		assertThat(tradeRankingService.getTotalTradeAmounts()).isEmpty();
		assertThat(tradeRankingService.getTradeAvgPrices()).isEmpty();
		assertThat(tradeRankingService.getTradeCounts()).isEmpty();

		verify(tradeHistoryRepository).findTotalTradeAmountByCompany(any(Pageable.class));
	}

	@Test
	@DisplayName("Double 타입 변환 테스트")
	void refreshCache_WithDoubleValues_ConvertsCorrectly() {
		List<Object[]> totalAmountData = new ArrayList<>();
		totalAmountData.add(new Object[]{"COMP1", 1000.0});

		List<Object[]> avgPriceData = new ArrayList<>();
		avgPriceData.add(new Object[]{"COMP1", 100.0});

		doReturn(totalAmountData).when(tradeHistoryRepository).findTotalTradeAmountByCompany(any(Pageable.class));
		doReturn(avgPriceData).when(tradeHistoryRepository).findTradeAvgPriceByCompany(any(Pageable.class));

		Company company1 = mock(Company.class);
		doReturn("회사1").when(company1).getIsuNm();
		doReturn(company1).when(companyRepository).findByIsuSrtCd("COMP1");

		tradeRankingService.refreshCache();

		List<TotalTradeAmountDto> totalAmounts = tradeRankingService.getTotalTradeAmounts();
		List<TradeAvgPriceDto> avgPrices = tradeRankingService.getTradeAvgPrices();

		assertThat(totalAmounts).hasSize(1);
		assertThat(totalAmounts.get(0).companyName()).isEqualTo("회사1");
		assertThat(totalAmounts.get(0).totalAmount()).isEqualByComparingTo(new BigDecimal("1000.0"));

		assertThat(avgPrices).hasSize(1);
		assertThat(avgPrices.get(0).companyName()).isEqualTo("회사1");
		assertThat(avgPrices.get(0).avgPrice()).isEqualByComparingTo(new BigDecimal("100.0"));

		verify(companyRepository, atLeastOnce()).findByIsuSrtCd("COMP1");
	}

	@Test
	@DisplayName("회사 정보를 찾을 수 없을 때 기본값 사용")
	void refreshCache_WhenCompanyNotFound_UsesDefaultName() {
		List<Object[]> totalAmountData = new ArrayList<>();
		totalAmountData.add(new Object[]{"UNKNOWN", 1000.0});

		doReturn(totalAmountData).when(tradeHistoryRepository).findTotalTradeAmountByCompany(any(Pageable.class));
		doThrow(new CompanyNotFound()).when(companyRepository).findByIsuSrtCd("UNKNOWN");

		tradeRankingService.refreshCache();

		List<TotalTradeAmountDto> result = tradeRankingService.getTotalTradeAmounts();

		assertThat(result).hasSize(1);
		assertThat(result.get(0).companyCode()).isEqualTo("UNKNOWN");
		assertThat(result.get(0).companyName()).isEqualTo("Unknown");
		assertThat(result.get(0).totalAmount()).isEqualByComparingTo(new BigDecimal("1000.0"));

		verify(companyRepository).findByIsuSrtCd("UNKNOWN");
	}
}
