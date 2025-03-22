package com.onseju.orderservice.company.repository;

import com.onseju.orderservice.company.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyJpaRepository extends JpaRepository<Company, Long> {

	List<Company> findByIsuNmContainingOrIsuAbbrvContainingOrIsuEngNmContainingOrIsuSrtCdContaining(
			String isuNm,
			String isuAbbrv,
			String isuEngNm,
			String isuSrtCd
	);

	Optional<Company> findByIsuSrtCd(String isuSrtCd);

}
