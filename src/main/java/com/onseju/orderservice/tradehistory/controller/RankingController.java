package com.onseju.orderservice.tradehistory.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.onseju.orderservice.tradehistory.dto.TotalTradeAmountDto;
import com.onseju.orderservice.tradehistory.dto.TradeAvgPriceDto;
import com.onseju.orderservice.tradehistory.dto.TradeCountDto;
import com.onseju.orderservice.tradehistory.service.TradeRankingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/rankings")
@RequiredArgsConstructor
public class RankingController {

	private final TradeRankingService rankingService;

	/**
	 * 모든 랭킹 데이터 한 번에 조회
	 */
	@GetMapping
	public ResponseEntity<RankingResponse> getAllRankings() {
		List<TotalTradeAmountDto> totalAmounts = rankingService.getTotalTradeAmounts();
		List<TradeAvgPriceDto> avgPrices = rankingService.getTradeAvgPrices();
		List<TradeCountDto> tradeCounts = rankingService.getTradeCounts();

		// 어느 하나라도 캐시가 비어있으면 갱신 요청
		if (totalAmounts.isEmpty() || avgPrices.isEmpty() || tradeCounts.isEmpty()) {
			rankingService.refreshCache();
			totalAmounts = rankingService.getTotalTradeAmounts();
			avgPrices = rankingService.getTradeAvgPrices();
			tradeCounts = rankingService.getTradeCounts();
		}

		RankingResponse response = new RankingResponse(totalAmounts, avgPrices, tradeCounts);
		return ResponseEntity.ok(response);
	}

}
