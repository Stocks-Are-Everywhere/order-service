package com.onseju.orderservice.stub;

import java.math.BigDecimal;
import java.util.List;

import com.onseju.orderservice.company.domain.Company;
import com.onseju.orderservice.company.service.repository.CompanyRepository;

public class StubCompanyRepository implements CompanyRepository {

	private final Company company = Company.builder()
			.isuCd("005930")
			.isuSrtCd("005930")
			.isuNm("삼성전자")
			.isuAbbrv("KOSPI")
			.isuEngNm("주권")
			.kindStkcertTpNm("삼성전자")
			.closingPrice(new BigDecimal(1000))
			.build();

	@Override
	public List<Company> findByIsuNmContainingOrIsuAbbrvContainingOrIsuEngNmContainingOrIsuSrtCdContaining(
			String query) {
		return List.of(company);
	}

	@Override
	public List<Company> findAll() {
		return List.of(company);
	}

	@Override
	public List<String> findAllIsuSrtCd() {
		return List.of(company.getIsuSrtCd());
	}

	@Override
	public void save(Company company) {

	}

	@Override
	public void saveAll(List<Company> companies) {

	}

	@Override
	public Company findByIsuSrtCd(String isuSrt) {
		return company;
	}

}
