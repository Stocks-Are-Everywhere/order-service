package com.onseju.orderservice.tradehistory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onseju.orderservice.global.jwt.JwtUtil;
import com.onseju.orderservice.global.security.UserDetailsServiceImpl;
import com.onseju.orderservice.tradehistory.dto.TotalTradeAmountDto;
import com.onseju.orderservice.tradehistory.dto.TradeAvgPriceDto;
import com.onseju.orderservice.tradehistory.dto.TradeCountDto;
import com.onseju.orderservice.tradehistory.service.TradeRankingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RankingController.class)
public class RankingControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TradeRankingService rankingService;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private JwtUtil jwtUtil;

	@MockitoBean
	UserDetailsServiceImpl userDetailsServiceImpl;

	private List<TotalTradeAmountDto> totalTradeAmounts;
	private List<TradeAvgPriceDto> tradeAvgPrices;
	private List<TradeCountDto> tradeCounts;

	@BeforeEach
	void setUp() {
		// 테스트 데이터 설정 - companyName 필드 추가
		totalTradeAmounts = Arrays.asList(
			new TotalTradeAmountDto("COMP1", "회사1", new BigDecimal("1000.00")),
			new TotalTradeAmountDto("COMP2", "회사2", new BigDecimal("800.00"))
		);

		tradeAvgPrices = Arrays.asList(
			new TradeAvgPriceDto("COMP3", "회사3", new BigDecimal("50.00")),
			new TradeAvgPriceDto("COMP4", "회사4", new BigDecimal("45.00"))
		);

		tradeCounts = Arrays.asList(
			new TradeCountDto("COMP5", "회사5", 100L),
			new TradeCountDto("COMP6", "회사6", 80L)
		);
	}

	@Test
	@DisplayName("캐시가 채워져 있을 때 모든 랭킹 데이터를 성공적으로 조회")
	void getAllRankings_WithCachePopulated_ReturnsAllRankings() throws Exception {
		// Given
		when(rankingService.getTotalTradeAmounts()).thenReturn(totalTradeAmounts);
		when(rankingService.getTradeAvgPrices()).thenReturn(tradeAvgPrices);
		when(rankingService.getTradeCounts()).thenReturn(tradeCounts);

		// When & Then
		mockMvc.perform(get("/api/rankings")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalTradeAmounts[0].companyCode").value("COMP1"))
			.andExpect(jsonPath("$.totalTradeAmounts[0].companyName").value("회사1"))
			.andExpect(jsonPath("$.totalTradeAmounts[0].totalAmount").value(1000.00))
			.andExpect(jsonPath("$.tradeAvgPrices[0].companyCode").value("COMP3"))
			.andExpect(jsonPath("$.tradeAvgPrices[0].companyName").value("회사3"))
			.andExpect(jsonPath("$.tradeAvgPrices[0].avgPrice").value(50.00))
			.andExpect(jsonPath("$.tradeCounts[0].companyCode").value("COMP5"))
			.andExpect(jsonPath("$.tradeCounts[0].companyName").value("회사5"))
			.andExpect(jsonPath("$.tradeCounts[0].count").value(100));

		// 캐시가 채워져 있으므로 refreshCache는 호출되지 않아야 함
		verify(rankingService, never()).refreshCache();
	}

	@Test
	@DisplayName("캐시가 비어있을 때 캐시 갱신 후 모든 랭킹 데이터를 조회")
	void getAllRankings_WithEmptyCache_RefreshesAndReturnsAllRankings() throws Exception {
		// Given
		// 처음에는 빈 캐시 반환
		when(rankingService.getTotalTradeAmounts()).thenReturn(Collections.emptyList())
			.thenReturn(totalTradeAmounts);
		when(rankingService.getTradeAvgPrices()).thenReturn(tradeAvgPrices);
		when(rankingService.getTradeCounts()).thenReturn(tradeCounts);

		// When & Then
		mockMvc.perform(get("/api/rankings")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalTradeAmounts[0].companyCode").value("COMP1"))
			.andExpect(jsonPath("$.totalTradeAmounts[0].companyName").value("회사1"))
			.andExpect(jsonPath("$.tradeAvgPrices[0].companyCode").value("COMP3"))
			.andExpect(jsonPath("$.tradeAvgPrices[0].companyName").value("회사3"))
			.andExpect(jsonPath("$.tradeCounts[0].companyCode").value("COMP5"))
			.andExpect(jsonPath("$.tradeCounts[0].companyName").value("회사5"));

		// 캐시가 비어있으므로 refreshCache가 호출되어야 함
		verify(rankingService, times(1)).refreshCache();
	}

	@Test
	@DisplayName("모든 캐시가 비어있을 때 캐시 갱신 후 모든 랭킹 데이터를 조회")
	void getAllRankings_WithAllEmptyCaches_RefreshesAndReturnsAllRankings() throws Exception {
		// Given
		// 처음에는 빈 캐시 반환, refreshCache 후에는 데이터 반환
		when(rankingService.getTotalTradeAmounts()).thenReturn(Collections.emptyList())
			.thenReturn(totalTradeAmounts);
		when(rankingService.getTradeAvgPrices()).thenReturn(Collections.emptyList())
			.thenReturn(tradeAvgPrices);
		when(rankingService.getTradeCounts()).thenReturn(Collections.emptyList())
			.thenReturn(tradeCounts);

		// When & Then
		mockMvc.perform(get("/api/rankings")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalTradeAmounts[0].companyCode").value("COMP1"))
			.andExpect(jsonPath("$.totalTradeAmounts[0].companyName").value("회사1"))
			.andExpect(jsonPath("$.tradeAvgPrices[0].companyCode").value("COMP3"))
			.andExpect(jsonPath("$.tradeAvgPrices[0].companyName").value("회사3"))
			.andExpect(jsonPath("$.tradeCounts[0].companyCode").value("COMP5"))
			.andExpect(jsonPath("$.tradeCounts[0].companyName").value("회사5"));

		// 모든 캐시가 비어있으므로 refreshCache가 호출되어야 함
		verify(rankingService, times(1)).refreshCache();
	}
}
