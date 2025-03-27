package com.onseju.orderservice.company.service.repository;

import java.util.List;

import com.onseju.orderservice.company.domain.Company;

public interface CompanyRepository {

	List<Company> findByIsuNmContainingOrIsuAbbrvContainingOrIsuEngNmContainingOrIsuSrtCdContaining(
			final String query);

	List<Company> findAll();

	List<String> findAllIsuSrtCd();

	void save(final Company company);

	void saveAll(final List<Company> companies);

	Company findByIsuSrtCd(final String isuSrt);
}
