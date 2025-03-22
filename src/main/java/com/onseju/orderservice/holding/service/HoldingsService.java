package com.onseju.orderservice.holding.service;

import com.onseju.orderservice.holding.domain.Holdings;
import com.onseju.orderservice.holding.service.dto.UpdateHoldingsParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class HoldingsService {

	private final HoldingsRepository holdingsRepository;

	public void updateHoldingsAfterTrade(final UpdateHoldingsParams params) {
		final Holdings holdings = getOrCreateHoldings(params.accountId(), params.companyCode());
		holdings.updateHoldings(params.type(), params.price(), params.quantity());
		holdingsRepository.save(holdings);
	}

	public Holdings getOrCreateHoldings(final Long accountId, final String companyCode) {
		return holdingsRepository.findByAccountIdAndCompanyCode(accountId, companyCode)
				.orElse(
						Holdings.builder()
						.accountId(accountId)
						.companyCode(companyCode)
						.quantity(BigDecimal.ZERO)
						.reservedQuantity(BigDecimal.ZERO)
						.averagePrice(BigDecimal.ZERO)
						.totalPurchasePrice(BigDecimal.ZERO)
						.build()
				);
	}
}
