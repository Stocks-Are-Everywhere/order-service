package com.onseju.orderservice.order.service;

import com.onseju.orderservice.client.UserServiceClients;
import com.onseju.orderservice.client.dto.OrderValidationResponse;
import com.onseju.orderservice.fake.FakeOrderRepository;
import com.onseju.orderservice.order.domain.Type;
import com.onseju.orderservice.order.exception.OrderPriceQuotationException;
import com.onseju.orderservice.order.exception.PriceOutOfRangeException;
import com.onseju.orderservice.order.mapper.OrderMapper;
import com.onseju.orderservice.order.service.dto.CreateOrderParams;
import com.onseju.orderservice.stub.StubCompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class OrderServiceTest {

	OrderService orderService;
	StubCompanyRepository companyRepository = new StubCompanyRepository();
	FakeOrderRepository orderRepository = new FakeOrderRepository();
	OrderMapper orderMapper = new OrderMapper();
	ApplicationEventPublisher applicationEventPublisher;
	UserServiceClients userServiceClients;

	@BeforeEach
	void setUp() {
		applicationEventPublisher = Mockito.mock(ApplicationEventPublisher.class);
		userServiceClients = Mockito.mock(UserServiceClients.class);
		orderService = new OrderService(orderRepository, companyRepository, orderMapper, userServiceClients, applicationEventPublisher);
	}

	@Nested
	class CreateOrderTest {

		@Test
		@DisplayName("TC20.2.1 주문 생성 테스트")
		void testPlaceOrder() {
			CreateOrderParams params = createCreateOrderParams(Type.LIMIT_BUY, new BigDecimal(1), new BigDecimal(1000), 1L);
			when(userServiceClients.validateOrderAndGetAccountId(any()))
					.thenReturn(new ResponseEntity<>(new OrderValidationResponse(1L), HttpStatus.OK));

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
			when(userServiceClients.validateOrderAndGetAccountId(any()))
					.thenReturn(new ResponseEntity<>(new OrderValidationResponse(1L), HttpStatus.OK));

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
			when(userServiceClients.validateOrderAndGetAccountId(any()))
					.thenReturn(new ResponseEntity<>(new OrderValidationResponse(1L), HttpStatus.OK));

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
	@DisplayName("user-service와의 통신 테스트")
	public class communicationWithUserService {

		@Test
		@DisplayName("")
		void communicationWithUserService() {
			// given
			BigDecimal price = new BigDecimal(1300);
			CreateOrderParams params = createCreateOrderParams(Type.LIMIT_BUY, new BigDecimal(1), price, 1L);
			when(userServiceClients.validateOrderAndGetAccountId(any()))
					.thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

			// when, then
			assertThatThrownBy(() -> orderService.placeOrder(params))
					.isInstanceOf(HttpClientErrorException.class);
		}
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
}
