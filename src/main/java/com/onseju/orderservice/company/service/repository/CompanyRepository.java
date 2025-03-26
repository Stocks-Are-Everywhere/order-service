package com.onseju.orderservice.company.service.repository;

import java.util.List;

import com.onseju.orderservice.company.domain.Company;

public interface CompanyRepository {

	List<Company> findByIsuNmContainingOrIsuAbbrvContainingOrIsuEngNmContainingOrIsuSrtCdContaining(
			final String query);

	List<Company> findAll();

	void save(final Company company);

	Company findByIsuSrtCd(final String isuSrt);
}
