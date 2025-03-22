package com.onseju.orderservice.holding.service;

import com.onseju.orderservice.holding.domain.Holdings;

import java.util.Optional;

public interface HoldingsRepository {

	Holdings getByAccountIdAndCompanyCode(final Long accountId, final String companyCode);

	Holdings save(final Holdings holdings);

	Optional<Holdings> findByAccountIdAndCompanyCode(final Long accountId, final String companyCode);
}
