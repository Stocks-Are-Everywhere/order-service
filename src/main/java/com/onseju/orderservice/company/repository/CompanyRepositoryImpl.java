package com.onseju.orderservice.company.repository;

import com.onseju.orderservice.company.domain.Company;
import com.onseju.orderservice.company.exception.CompanyNotFound;
import com.onseju.orderservice.company.service.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class CompanyRepositoryImpl implements CompanyRepository {

	private final CompanyJpaRepository companyJpaRepository;

	/**
	 * 이름 또는 단축코드를 포함하는 회사 목록을 조회하는 메서드
	 *
	 * @param query 검색어
	 * @return 검색된 회사 리스트
	 */
	@Override
	public List<Company> findByIsuNmContainingOrIsuAbbrvContainingOrIsuEngNmContainingOrIsuSrtCdContaining(
			final String query) {
		return companyJpaRepository.findByIsuNmContainingOrIsuAbbrvContainingOrIsuEngNmContainingOrIsuSrtCdContaining(
				query,
				query, query, query);
	}

	@Override
	public List<Company> findAll() {
		return companyJpaRepository.findAll();
	}

	@Override
	public void save(final Company company) {
		companyJpaRepository.save(company);
	}

	@Override
	public Company findByIsuSrtCd(final String isuSrt) {
		return companyJpaRepository.findByIsuSrtCd(isuSrt)
				.orElseThrow(CompanyNotFound::new);
	}
}
