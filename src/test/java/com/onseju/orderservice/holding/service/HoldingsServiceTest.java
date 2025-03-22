package com.onseju.orderservice.holding.service;

import com.onseju.orderservice.holding.domain.Holdings;
import com.onseju.orderservice.holding.service.dto.UpdateHoldingsParams;
import com.onseju.orderservice.order.domain.Type;
import com.onseju.orderservice.fake.FakeHoldingsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class HoldingsServiceTest {

    private static final Long ACCOUNT_ID = 1L;
    private static final String COMPANY_CODE = "005930";
    private final HoldingsRepository holdingsRepository = new FakeHoldingsRepository();
    private final HoldingsService holdingsService = new HoldingsService(holdingsRepository);

    @Test
    @DisplayName("보유 내역이 존재할 경우 이전 보유 내역과 합친다.")
    void getExistingHoldings() {
        // given
        Holdings holdings = createHoldings(new BigDecimal("100"));
        holdingsRepository.save(holdings);
        UpdateHoldingsParams params = new UpdateHoldingsParams(Type.LIMIT_SELL, ACCOUNT_ID, COMPANY_CODE, new BigDecimal(1000), new BigDecimal(10));

        // when
        holdingsService.updateHoldingsAfterTrade(params);

        // then
        Holdings updatedHoldings = holdingsRepository.getByAccountIdAndCompanyCode(ACCOUNT_ID, COMPANY_CODE);
        assertThat(updatedHoldings.getReservedQuantity()).isEqualTo(BigDecimal.ZERO);
        assertThat(updatedHoldings.getQuantity()).isEqualTo(new BigDecimal(90));
    }

    @Test
    @DisplayName("보유 내역이 존재하지 않을 경우 새롭게 생성하여 저장한다.")
    void createHoldings() {
        // given
        UpdateHoldingsParams params = new UpdateHoldingsParams(Type.LIMIT_BUY, ACCOUNT_ID, COMPANY_CODE, new BigDecimal(1000), new BigDecimal(10));

        // when
        holdingsService.updateHoldingsAfterTrade(params);

        // then
        Holdings updatedHoldings = holdingsRepository.getByAccountIdAndCompanyCode(ACCOUNT_ID, COMPANY_CODE);
        assertThat(updatedHoldings.getCompanyCode()).isEqualTo(COMPANY_CODE);
        assertThat(updatedHoldings.getReservedQuantity()).isEqualTo(BigDecimal.ZERO);
        assertThat(updatedHoldings.getQuantity()).isEqualTo(new BigDecimal(10));
    }

    private Holdings createHoldings(BigDecimal quantity) {
        return Holdings.builder()
                .companyCode("005930")
                .quantity(quantity)
                .reservedQuantity(new BigDecimal(10))
                .averagePrice(new BigDecimal(1000))
                .totalPurchasePrice(new BigDecimal(10000))
                .accountId(ACCOUNT_ID)
                .build();
    }
}