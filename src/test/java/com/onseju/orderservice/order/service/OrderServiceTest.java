package com.onseju.orderservice.order.service;

import com.onseju.orderservice.company.service.CompanyRepository;
import com.onseju.orderservice.fake.FakeHoldingsRepository;
import com.onseju.orderservice.fake.FakeOrderRepository;
import com.onseju.orderservice.holding.domain.Holdings;
import com.onseju.orderservice.holding.exception.HoldingsNotFoundException;
import com.onseju.orderservice.holding.exception.InsufficientHoldingsException;
import com.onseju.orderservice.holding.service.HoldingsRepository;
import com.onseju.orderservice.order.controller.request.OrderRequest;
import com.onseju.orderservice.order.domain.Type;
import com.onseju.orderservice.order.exception.OrderPriceQuotationException;
import com.onseju.orderservice.order.exception.PriceOutOfRangeException;
import com.onseju.orderservice.order.mapper.OrderMapper;
import com.onseju.orderservice.order.service.dto.CreateOrderParams;
import com.onseju.orderservice.order.service.repository.AccountRepository;
import com.onseju.orderservice.order.service.repository.OrderRepository;
import com.onseju.orderservice.stub.StubAccountRepository;
import com.onseju.orderservice.stub.StubCompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderServiceTest {

	OrderService orderService;

	AccountRepository accountRepository = new StubAccountRepository();;

	CompanyRepository companyRepository = new StubCompanyRepository();

	HoldingsRepository holdingsRepository = new FakeHoldingsRepository();

	OrderRepository orderRepository = new FakeOrderRepository();

	OrderMapper orderMapper = new OrderMapper();

	ApplicationEventPublisher applicationEventPublisher;

	@BeforeEach
	void setUp() {
		applicationEventPublisher = Mockito.mock(ApplicationEventPublisher.class);
		orderService = new OrderService(orderRepository, companyRepository, holdingsRepository, accountRepository, orderMapper, applicationEventPublisher);
	}

	@Nested
	class CreateOrderTest {

		@Test
		@DisplayName("TC20.2.1 주문 생성 테스트")
		void testPlaceOrder() {
			CreateOrderParams params = createCreateOrderParams(Type.LIMIT_BUY, new BigDecimal(1), new BigDecimal(1000), 1L);

			assertThatNoException().isThrownBy(() -> orderService.placeOrder(params));
		}
	}

	@Nested
	@DisplayName("입력된 가격에 대한 검증을 진행한다.")
	class BoundaryTests {

		@Test
		@DisplayName("입력된 가격이 종가 기준 상향 30% 이상일 경우 정상적으로 처리한다.")
		void placeOrderWhenPriceWithinUpperLimit() {
			// given
			BigDecimal price = new BigDecimal(1300);
			CreateOrderParams params = createCreateOrderParams(Type.LIMIT_BUY, new BigDecimal(1), price, 1L);

			// when, then
			assertThatNoException().isThrownBy(() -> orderService.placeOrder(params));
		}

		@Test
		@DisplayName("입력된 가격이 종가 기준 상향 30%를 초과할 경우 예외가 발생한다.")
		void throwExceptionWhenPriceExceedsUpperLimit() {
			// given
			BigDecimal price = new BigDecimal(1301);
			CreateOrderParams params = createCreateOrderParams(Type.LIMIT_SELL, new BigDecimal(10), price, 1L);

			// when, then
			assertThatThrownBy(() -> orderService.placeOrder(params)).isInstanceOf(PriceOutOfRangeException.class);
		}

		@Test
		@DisplayName("입력된 가격이 종가 기준 하향 30% 이하일 경우 정상적으로 처리한다.")
		void placeOrderWhenPriceWithinLowerLimit() {
			// given
			BigDecimal price = new BigDecimal(700);
			CreateOrderParams params = createCreateOrderParams(Type.LIMIT_BUY, new BigDecimal(10), price, 1L);

			// when, then
			assertThatNoException().isThrownBy(() -> orderService.placeOrder(params));
		}

		@Test
		@DisplayName("입력된 가격이 종가 기준 하향 30% 미만일 경우 예외가 발생한다.")
		void throwExceptionWhenPriceIsBelowLowerLimit() {
			// given
			BigDecimal price = new BigDecimal(699);
			CreateOrderParams params = createCreateOrderParams(Type.LIMIT_BUY, new BigDecimal(10), price, 1L);

			// when, then
			assertThatThrownBy(() -> orderService.placeOrder(params))
					.isInstanceOf(PriceOutOfRangeException.class);
		}

		@Test
		@DisplayName("입력 가격이 음수일 경우 예외가 발생한다.")
		void throwExceptionWhenInvalidPrice() {
			// given
			BigDecimal price = new BigDecimal(-1);
			CreateOrderParams params = createCreateOrderParams(Type.LIMIT_BUY, new BigDecimal(10), price, 1L);

			// when, then
			assertThatThrownBy(() -> orderService.placeOrder(params)).isInstanceOf(OrderPriceQuotationException.class);
		}

		@Test
		@DisplayName("유효하지 않은 단위의 가격이 입력될 경우 예외가 발생한다.")
		void throwExceptionWhenInvalidUnitPrice() {
			// given
			BigDecimal price = new BigDecimal("0.5");
			CreateOrderParams params = createCreateOrderParams(Type.LIMIT_BUY, new BigDecimal(10), price, 1L);

			// when, then
			assertThatThrownBy(() -> orderService.placeOrder(params)).isInstanceOf(OrderPriceQuotationException.class);
		}
	}

	@Nested
	@DisplayName("매도 주문일 경우 보유 주식 개수를 확인하고, 예약 주문 개수를 저장한다.")
	class SellOrderReservation {

		@Test
		@DisplayName("매도 주문일 경우, 예약 주문 개수를 저장한다.")
		void reserveForSellType() {
		    // given
			CreateOrderParams params = createCreateOrderParams(Type.LIMIT_SELL, new BigDecimal(1), new BigDecimal(1000), 1L);
			Holdings holdings = createHoldings(new BigDecimal(10));
			holdingsRepository.save(holdings);

			// when
			orderService.placeOrder(params);

		    // then
			Holdings updatedHoldings = holdingsRepository.getByAccountIdAndCompanyCode(holdings.getAccountId(), holdings.getCompanyCode());
			assertThat(updatedHoldings.getReservedQuantity()).isEqualTo(new BigDecimal(1));
		}

		@Test
		@DisplayName("매도 주문일 경우, 입력한 종목에 대한 보유 주식이 없을 경우 예외가 발생한다.")
		void throwExceptionWhenSellingStockWithoutHoldingAny() {
			// given
			CreateOrderParams params = createCreateOrderParams(Type.LIMIT_SELL, new BigDecimal(1), new BigDecimal(1000), 1L);

			// when, then
			assertThatThrownBy(() -> orderService.placeOrder(params))
					.isInstanceOf(HoldingsNotFoundException.class);
		}

		@Test
		@DisplayName("매도 주문일 경우, 입력한 종목에 대한 보유 주식의 개수가 부족할 경우 예외가 발생한다.")
		void throwExceptionWhenSellingExceedingOwnedQuantity() {
			// given
			CreateOrderParams params = createCreateOrderParams(Type.LIMIT_SELL, new BigDecimal(100), new BigDecimal(1000), 1L);
			Holdings holdings = createHoldings(new BigDecimal(10));
			holdingsRepository.save(holdings);

			// when, then
			assertThatThrownBy(() -> orderService.placeOrder(params))
					.isInstanceOf(InsufficientHoldingsException.class);
		}
	}

	private OrderRequest createOrderRequest(
		Type type,
		BigDecimal totalQuantity,
		BigDecimal price
	) {
		return new OrderRequest(
			"005930",
			type,
			totalQuantity,
			price,
			LocalDateTime.of(2025, 1, 1, 1, 1)
		);
	}

	private CreateOrderParams createCreateOrderParams(
			Type type,
			BigDecimal totalQuantity,
			BigDecimal price,
			Long memberId
	) {
		return new CreateOrderParams(
				"005930",
				type,
				totalQuantity,
				price,
				LocalDateTime.of(2025, 1, 1, 1, 1),
				memberId
		);
	}

	private Holdings createHoldings(BigDecimal quantity) {
		return Holdings.builder()
				.companyCode("005930")
				.quantity(quantity)
				.reservedQuantity(new BigDecimal(0))
				.averagePrice(new BigDecimal(1000))
				.totalPurchasePrice(new BigDecimal(10000))
				.accountId(1L)
				.build();
	}
}
