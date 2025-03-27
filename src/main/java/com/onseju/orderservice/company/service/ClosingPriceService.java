package com.onseju.orderservice.company.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.onseju.orderservice.company.service.repository.CompanyRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 종가 정보를 관리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClosingPriceService {
	private final Map<String, BigDecimal> closingPriceMap = new ConcurrentHashMap<>();
	private final CompanyRepository companyRepository;

	/**
	 * 종가 조회
	 */
	public BigDecimal getClosingPrice(final String companyCode) {
		return closingPriceMap.get(companyCode);
	}

	/**
	 * 종가 일괄 업데이트
	 */
	public void updateClosingPrices(Map<String, BigDecimal> priceUpdates) {
		closingPriceMap.putAll(priceUpdates);
	}

	/**
	 * 모든 종목 코드 가져오기
	 */
	public List<String> getAllCompanyCode() {
		return companyRepository.findAllIsuSrtCd();
	}

	/**
	 * 캐시 초기화
	 */
	public void clearCache() {
		closingPriceMap.clear();
	}
}
