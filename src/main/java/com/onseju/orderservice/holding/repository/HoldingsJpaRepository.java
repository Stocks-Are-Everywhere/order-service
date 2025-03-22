package com.onseju.orderservice.holding.repository;

import com.onseju.orderservice.holding.domain.Holdings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HoldingsJpaRepository extends JpaRepository<Holdings, Long> {
	Optional<Holdings> findByAccountIdAndCompanyCode(final Long accountId, final String companyCode);
}
