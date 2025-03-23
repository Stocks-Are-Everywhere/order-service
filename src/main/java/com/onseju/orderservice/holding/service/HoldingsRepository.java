package com.onseju.orderservice.holding.service;

import com.onseju.orderservice.holding.domain.Holdings;

public interface HoldingsRepository {

	Holdings getByAccountIdAndCompanyCode(final Long accountId, final String companyCode);
}
