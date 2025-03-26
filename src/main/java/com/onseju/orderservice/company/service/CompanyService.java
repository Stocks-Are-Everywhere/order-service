package com.onseju.orderservice.company.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.onseju.orderservice.company.controller.response.CompanySearchResponse;
import com.onseju.orderservice.company.mapper.CompanyMapper;
import com.onseju.orderservice.company.service.repository.CompanyRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyService {

	private final CompanyRepository companyRepository;
	private final CompanyMapper companyMapper;

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
