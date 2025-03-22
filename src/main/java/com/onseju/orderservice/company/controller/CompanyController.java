package com.onseju.orderservice.company.controller;

import com.onseju.orderservice.company.controller.response.CompanySearchResponse;
import com.onseju.orderservice.company.service.CompanyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

	private final CompanyService companyService;

	public CompanyController(final CompanyService companyService) {
		this.companyService = companyService;
	}

	/**
	 * 검색어를 기반으로 회사를 조회하는 메서드
	 *
	 * @param query 검색어 (회사명 또는 코드)
	 * @return 검색된 회사 리스트 또는 400 Bad Request (빈 쿼리인 경우)
	 */

	@GetMapping("/search")
	public List<CompanySearchResponse> searchCompanies(@RequestParam(name = "query") final String query) {
		if (query == null || query.trim().isEmpty()) {
			return Collections.emptyList();
		}
		return companyService.searchCompanies(query);
	}
}

