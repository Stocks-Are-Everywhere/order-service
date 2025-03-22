package com.onseju.orderservice.company.controller.response;

/**
 * 회사 검색 결과를 담는 DTO
 */
public record CompanySearchResponse(
		String isuNm,
		String isuSrtCd,
		String isuAbbrv,
		String isEngNm,
		String kindstkcertTpNm
) {
}
