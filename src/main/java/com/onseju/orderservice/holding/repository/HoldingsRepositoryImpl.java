package com.onseju.orderservice.holding.repository;

import com.onseju.orderservice.holding.domain.Holdings;
import com.onseju.orderservice.holding.exception.HoldingsNotFoundException;
import com.onseju.orderservice.holding.service.HoldingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class HoldingsRepositoryImpl implements HoldingsRepository {

    private final HoldingsJpaRepository holdingsJpaRepository;

    @Override
    public Optional<Holdings> findByAccountIdAndCompanyCode(final Long accountId, final String companyCode) {
        return holdingsJpaRepository.findByAccountIdAndCompanyCode(accountId, companyCode);
    }

    @Override
    public Holdings save(final Holdings holdings) {
        return holdingsJpaRepository.save(holdings);
    }

    @Override
    public Holdings getByAccountIdAndCompanyCode(final Long accountId, final String companyCode) {
        return holdingsJpaRepository.findByAccountIdAndCompanyCode(accountId, companyCode)
                .orElseThrow(HoldingsNotFoundException::new);
    }


}
