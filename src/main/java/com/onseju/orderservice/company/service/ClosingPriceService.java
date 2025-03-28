package com.onseju.orderservice.company.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

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
	 * 인메모리에서 모든 종목 코드 가져오기
	 */
	public Set<String> getAllCompanyCodeByInmemory() {
		return closingPriceMap.keySet();
	}

	/**
	 * 캐시 초기화
	 */
	public void clearCache() {
		closingPriceMap.clear();
	}
}
