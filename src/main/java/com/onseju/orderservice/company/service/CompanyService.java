package com.onseju.orderservice.company.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onseju.orderservice.chart.service.ChartService;
import com.onseju.orderservice.company.controller.response.CompanySearchResponse;
import com.onseju.orderservice.company.domain.Company;
import com.onseju.orderservice.company.mapper.CompanyMapper;
import com.onseju.orderservice.company.service.repository.CompanyRepository;
import com.onseju.orderservice.tradehistory.domain.TradeHistory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyService {

	private final CompanyRepository companyRepository;
	private final CompanyMapper companyMapper;
	private final ChartService chartService;
	private final ClosingPriceService closingPriceService;

	private static final BigDecimal DEFAULT_PRICE = BigDecimal.valueOf(100);

	@Scheduled(cron = "0 0 6 * * *") // 매일 오전 6시 갱신
	public void refreshClosingPrices() {
		log.info("종가 데이터 업데이트 및 매시 갱신 시작");
		try {
			// 전일 거래 종료 시점의 가격으로 Company 엔티티 및 캐시 업데이트
			updateCompanyClosingPricesAndCache();
		} catch (Exception e) {
			log.error("종가 데이터 업데이트 중 오류 발생");
		}
	}

	/**
	 * 전일 거래 종료 시점의 가격을 기준으로 Company 엔티티의 closingPrice 업데이트
	 * 메모리에 캐싱된 최근 거래 내역 활용
	 */
	@Transactional
	private void updateCompanyClosingPricesAndCache() {
		log.info("Company 엔티티 종가 데이터 업데이트 시작");

		// 모든 종목 코드 조회
		final List<Company> allCompanies = companyRepository.findAll();
		final List<Company> updatedCompanies = new ArrayList<>();
		final Map<String, BigDecimal> updatedPrices = new ConcurrentHashMap<>();

		// 캐시 초기화
		closingPriceService.clearCache();

		// 각 종목의 종가 정보 업데이트
		processCompanyClosingPrices(allCompanies, updatedCompanies, updatedPrices);

		// 종가 캐시 일괄 업데이트
		closingPriceService.updateClosingPrices(updatedPrices);

		// 업데이트된 엔티티들 저장
		saveUpdatedCompanies(updatedCompanies);
	}

	private void processCompanyClosingPrices(
			final List<Company> companies,
			final List<Company> updatedCompanies,
			final Map<String, BigDecimal> updatedPrices
	) {
		for (Company company : companies) {
			final String companyCode = company.getIsuSrtCd();
			try {
				updateSingleCompanyClosingPrice(company, companyCode, updatedCompanies, updatedPrices);
			} catch (Exception e) {
				log.error("종목 {} 종가 업데이트 중 오류 발생: {}", companyCode, e.getMessage());
			}
		}

	}

	/**
	 * 개별 회사의 종가 정보 업데이트
	 */
	private void updateSingleCompanyClosingPrice(
			final Company company,
			final String companyCode,
			final List<Company> updatedCompanies,
			final Map<String, BigDecimal> updatedPrices
	) {
		// 메모리에 캐싱된 마지막 거래 내역 조회
		final Optional<TradeHistory> lastTrade = chartService.getLastTrade(companyCode);

		if (lastTrade.isPresent()) {
			// 최근 거래가 있는 경우 해당 가격으로 업데이트
			updateWithLatestTradePrice(company, lastTrade.get().getPrice(), updatedCompanies, updatedPrices);
		} else {
			// 거래 가격이 없는 경우 기존 가격 유지 또는 기본값 사용
			updateWithDefaultOrExistingPrice(company, companyCode, updatedCompanies, updatedPrices);

		}
	}

	/**
	 * 최근 거래 가격을 기준으로 종가 업데이트
	 */
	private void updateWithLatestTradePrice(
			final Company company,
			final BigDecimal lastPrice,
			final List<Company> updatedCompanies,
			final Map<String, BigDecimal> updatedPrices
	) {
		company.updateClosingPrice(lastPrice);
		updatedCompanies.add(company);
		updatedPrices.put(company.getIsuSrtCd(), lastPrice);
	}

	/**
	 * 거래 내역이 없는 경우 기존 가격 또는 기본값으로 종가 업데이트
	 */
	private void updateWithDefaultOrExistingPrice(
			final Company company,
			final String companyCode,
			final List<Company> updatedCompanies,
			final Map<String, BigDecimal> updatedPrices
	) {
		final BigDecimal existingPrice = Optional.ofNullable(company.getClosingPrice())
				.orElse(DEFAULT_PRICE);
		updatedPrices.put(companyCode, existingPrice);

		if (company.getClosingPrice() == null) {
			company.updateClosingPrice(DEFAULT_PRICE);
			updatedCompanies.add(company);
		}
	}

	/**
	 * 업데이트된 Company 엔티티들을 저장
	 */
	private void saveUpdatedCompanies(final List<Company> updatedCompanies) {
		if (!updatedCompanies.isEmpty()) {
			companyRepository.saveAll(updatedCompanies);
			log.info("종가 정보 DB 업데이트 완료");
		} else {
			log.info("업데이트할 종가 정보가 없습니다.");
		}
	}

	/**
	 * 검색어를 포함하는 회사 목록을 조회하는 메서드
	 *
	 * @param query 검색어
	 * @return 검색된 회사 리스트
	 */
	public List<CompanySearchResponse> searchCompanies(final String query) {
		return companyRepository.findByIsuNmContainingOrIsuAbbrvContainingOrIsuEngNmContainingOrIsuSrtCdContaining(
						query)
				.stream()
				.map(companyMapper::toCompanySearchResponse)
				.toList();
	}

}
