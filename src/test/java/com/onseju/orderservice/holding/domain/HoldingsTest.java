package com.onseju.orderservice.holding.domain;

import com.onseju.orderservice.holding.exception.HoldingsNotFoundException;
import com.onseju.orderservice.holding.exception.InsufficientHoldingsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HoldingsTest {

    private Holdings holdings;

    @BeforeEach
    void setUp() {
        holdings = Holdings.builder()
                .quantity(BigDecimal.valueOf(100))
                .reservedQuantity(BigDecimal.valueOf(0))
                .averagePrice(BigDecimal.valueOf(1000))
                .totalPurchasePrice(BigDecimal.valueOf(100000))
                .build();
    }

    @Test
    @DisplayName("보유 주식 내역이 존재하는지 검증한다.")
    void validateExistHoldings() {
        // When & Then
        assertThatNoException()
                .isThrownBy(() -> holdings.validateExistHoldings());
    }

    @Test
    @DisplayName("보유 주식 수량이 충분한지 검증한다.")
    void validateEnoughHoldings_whenEnoughQuantity_shouldNotThrowException() {
        // Given
        BigDecimal checkQuantity = BigDecimal.valueOf(50);

        // When & Then
        assertThatCode(() -> holdings.validateEnoughHoldings(checkQuantity))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("보유 주식 수량이 부족할 경우 InsufficientHoldingsException이 발생해야 한다")
    void validateEnoughHoldings_whenNotEnoughQuantity_shouldThrowInsufficientHoldingsException() {
        // Given
        BigDecimal checkQuantity = BigDecimal.valueOf(110);

        // When & Then
        assertThatThrownBy(() -> holdings.validateEnoughHoldings(checkQuantity))
                .isInstanceOf(InsufficientHoldingsException.class)
                .hasMessage("판매 가능한 보유 주식 수량이 부족합니다.");
    }


    @Test
    @DisplayName("주식 수량이 0일 경우 validateExistHoldings 호출 시 HoldingsNotFoundException이 발생해야 한다")
    void validateExistHoldings_whenQuantityZero_shouldThrowHoldingsNotFoundException() {
        // Given
        holdings = Holdings.builder()
                .quantity(BigDecimal.valueOf(0))
                .reservedQuantity(BigDecimal.valueOf(20))
                .averagePrice(BigDecimal.valueOf(100))
                .totalPurchasePrice(BigDecimal.valueOf(2000))
                .build();

        // When & Then
        assertThatThrownBy(() -> holdings.validateExistHoldings())
                .isInstanceOf(HoldingsNotFoundException.class);
    }
}
