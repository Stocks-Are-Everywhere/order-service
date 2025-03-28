package com.onseju.orderservice.tradehistory.service;

import com.onseju.orderservice.company.domain.Company;
import com.onseju.orderservice.company.exception.CompanyNotFound;
import com.onseju.orderservice.company.service.repository.CompanyRepository;
import com.onseju.orderservice.tradehistory.dto.TotalTradeAmountDto;
import com.onseju.orderservice.tradehistory.dto.TradeAvgPriceDto;
import com.onseju.orderservice.tradehistory.dto.TradeCountDto;
import com.onseju.orderservice.tradehistory.service.repository.TradeHistoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TradeRankingService {

	private final TradeHistoryRepository tradeHistoryRepository;
	private final CompanyRepository companyRepository;  // 추가

	private final List<TotalTradeAmountDto> totalTradeAmountCache = new ArrayList<>();
	private final List<TradeAvgPriceDto> tradeAvgPriceCache = new ArrayList<>();
	private final List<TradeCountDto> tradeCountCache = new ArrayList<>();

	@Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시에 실행
	public synchronized void refreshCache() {
		log.info("거래 통계 캐시 갱신 시작");

		try {
			Pageable top10 = PageRequest.of(0, 10);

			List<TotalTradeAmountDto> totalAmountDtos = tradeHistoryRepository.findTotalTradeAmountByCompany(top10)
				.stream()
				.map(result -> {
					String companyCode = (String) result[0];
					String companyName = getCompanyName(companyCode);
					return new TotalTradeAmountDto(
						companyCode,
						companyName,
						result[1] instanceof Double ?
							BigDecimal.valueOf((Double) result[1]) :
							(BigDecimal) result[1]
					);
				})
				.collect(Collectors.toList());

			List<TradeAvgPriceDto> avgPriceDtos = tradeHistoryRepository.findTradeAvgPriceByCompany(top10)
				.stream()
				.map(result -> {
					String companyCode = (String) result[0];
					String companyName = getCompanyName(companyCode);
					return new TradeAvgPriceDto(
						companyCode,
						companyName,
						result[1] instanceof Double ?
							BigDecimal.valueOf((Double) result[1]) :
							(BigDecimal) result[1]
					);
				})
				.collect(Collectors.toList());

			List<TradeCountDto> countDtos = tradeHistoryRepository.findTradeCountByCompany(top10)
				.stream()
				.map(result -> {
					String companyCode = (String) result[0];
					String companyName = getCompanyName(companyCode);
					return new TradeCountDto(
						companyCode,
						companyName,
						((Number) result[1]).longValue()
					);
				})
				.collect(Collectors.toList());

			updateCache(totalTradeAmountCache, totalAmountDtos);
			updateCache(tradeAvgPriceCache, avgPriceDtos);
			updateCache(tradeCountCache, countDtos);

			log.info("거래 통계 캐시 갱신 완료. 총 거래액: {}, 평균 가격: {}, 거래 건수: {}",
				totalAmountDtos.size(), avgPriceDtos.size(), countDtos.size());
		} catch (Exception e) {
			log.error("거래 통계 캐시 갱신 중 오류 발생", e);
		}
	}

	// 회사 코드로 회사명을 조회하는 메서드
	private String getCompanyName(String companyCode) {
		try {
			Company company = companyRepository.findByIsuSrtCd(companyCode);
			return company.getIsuNm(); // 회사명 필드명은 실제 Company 엔티티에 맞게 수정
		} catch (CompanyNotFound e) {
			log.warn("회사 코드 {}에 해당하는 회사를 찾을 수 없습니다.", companyCode);
			return "Unknown"; // 회사를 찾을 수 없는 경우 기본값
		} catch (Exception e) {
			log.error("회사명 조회 중 오류 발생: {}", e.getMessage());
			return "Error"; // 오류 발생 시 기본값
		}
	}

	private <T> void updateCache(List<T> cache, List<T> newData) {
		cache.clear();
		cache.addAll(newData);
	}

	public List<TotalTradeAmountDto> getTotalTradeAmounts() {
		return new ArrayList<>(totalTradeAmountCache);
	}

	public List<TradeAvgPriceDto> getTradeAvgPrices() {
		return new ArrayList<>(tradeAvgPriceCache);
	}

	public List<TradeCountDto> getTradeCounts() {
		return new ArrayList<>(tradeCountCache);
	}
}
