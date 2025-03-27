package com.onseju.orderservice.company.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.onseju.orderservice.company.controller.response.CompanySearchResponse;
import com.onseju.orderservice.company.domain.Company;
import com.onseju.orderservice.company.service.repository.CompanyRepository;


@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

	@InjectMocks
	private CompanyService companyService;

	@Mock
	private CompanyRepository companyRepository;

	private final CompanyMapper companyMapper = new CompanyMapper();

	@BeforeEach
	void setUp() {
		companyService = new CompanyService(companyRepository, companyMapper);
	}

	@Test
	@DisplayName("회사 이름을 검색한다.")
	public void searchCompany() {
		// given
		String keyword = "삼";
		Company company = Company.builder()
				.isuCd("005930")
				.isuSrtCd("005930")
				.isuNm("삼성전자")
				.isuAbbrv("KOSPI")
				.isuEngNm("주권")
				.kindStkcertTpNm("삼성전자")
				.closingPrice(new BigDecimal(1000))
				.build();
		when(companyRepository.findByIsuNmContainingOrIsuAbbrvContainingOrIsuEngNmContainingOrIsuSrtCdContaining(
				"삼")).thenReturn(List.of(company));

		// when
		List<CompanySearchResponse> response = companyService.searchCompanies(keyword);

		assertThat(response).hasSize(1);
	}
}
