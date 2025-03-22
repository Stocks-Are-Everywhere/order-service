package com.onseju.orderservice.holding.domain;

import com.onseju.orderservice.holding.exception.HoldingsNotFoundException;
import com.onseju.orderservice.holding.exception.InsufficientHoldingsException;
import com.onseju.orderservice.order.domain.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    @DisplayName("예약 주문 처리 시 예약된 주식 수량이 업데이트 되어야 한다")
    void processReservedOrder_shouldUpdateReservedQuantity() {
        // Given
        BigDecimal reservedQuantity = BigDecimal.valueOf(10);

        // When
        holdings.reserveOrder(reservedQuantity);

        // Then
        assertThat(holdings.getReservedQuantity()).isEqualTo(BigDecimal.valueOf(10));
    }

    @Test
    @DisplayName("매수 시 보유 주식 수량과 총 매수 금액이 업데이트 되어야 한다")
    void updateHoldings_whenBuy_shouldUpdateQuantityAndTotalPurchasePrice() {
        // Given
        BigDecimal updatePrice = BigDecimal.valueOf(100);
        BigDecimal updateQuantity = BigDecimal.valueOf(10);

        // When
        holdings.updateHoldings(Type.LIMIT_BUY, updatePrice, updateQuantity);

        // Then
        assertThat(holdings.getQuantity()).isEqualTo(BigDecimal.valueOf(110));
        assertThat(holdings.getTotalPurchasePrice()).isEqualTo(BigDecimal.valueOf(101_000));
        assertThat(holdings.getAveragePrice()).isEqualTo(new BigDecimal("918.1818"));
    }

    @Test
    @DisplayName("매도 시 보유 주식 수량과 총 매수 금액이 업데이트 되어야 한다")
    void updateHoldings_whenSell_shouldUpdateQuantityAndTotalPurchasePrice() {
        // Given
        BigDecimal updateQuantity = BigDecimal.valueOf(1);
        BigDecimal originalTotalQuantity = holdings.getTotalPurchasePrice();

        // When
        holdings.updateHoldings(Type.LIMIT_SELL, BigDecimal.ZERO, updateQuantity);

        // Then
        assertThat(holdings.getQuantity()).isEqualTo(BigDecimal.valueOf(99));
        assertThat(holdings.getTotalPurchasePrice().intValueExact()).isEqualTo(originalTotalQuantity.subtract(updateQuantity.multiply(holdings.getAveragePrice())).intValueExact());
    }

    @Test
    @DisplayName("보유한 주식을 모두 판매한 경우 삭제한다.")
    void deleteHoldings() {
        // given
        BigDecimal quantity = BigDecimal.valueOf(100);

        // when
        holdings.updateHoldings(Type.LIMIT_SELL, new BigDecimal(1000), quantity);

        // then
        assertThat(holdings.getDeletedDateTime()).isNotNull();
    }

    @Test
    @DisplayName("보유한 주식의 총 금액을 계산한다.")
    void calculateTotalPurchasePrice() {
        // given
        BigDecimal updatedQuantity = BigDecimal.valueOf(50);
        BigDecimal totalPurchasePrice = holdings.getTotalPurchasePrice();
        BigDecimal averagePrice = holdings.getAveragePrice();

        // when
        holdings.updateHoldings(Type.LIMIT_SELL, new BigDecimal(1000), updatedQuantity);

        // then
        assertThat(holdings.getTotalPurchasePrice()).isEqualTo(totalPurchasePrice.subtract(averagePrice.multiply(updatedQuantity)));
    }
}