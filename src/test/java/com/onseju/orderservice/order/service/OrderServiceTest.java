package com.onseju.orderservice.order.service;

import com.onseju.orderservice.events.publisher.MatchedEventPublisher;
import com.onseju.orderservice.events.publisher.OrderEventPublisher;
import com.onseju.orderservice.fake.FakeOrderRepository;
import com.onseju.orderservice.global.response.ApiResponse;
import com.onseju.orderservice.global.utils.TsidGenerator;
import com.onseju.orderservice.order.client.UserServiceClient;
import com.onseju.orderservice.order.controller.resposne.OrderResponse;
import com.onseju.orderservice.order.domain.Type;
import com.onseju.orderservice.order.dto.BeforeTradeOrderDto;
import com.onseju.orderservice.order.dto.OrderValidationResponse;
import com.onseju.orderservice.order.exception.OrderPriceQuotationException;
import com.onseju.orderservice.order.exception.PriceOutOfRangeException;
import com.onseju.orderservice.order.mapper.OrderMapper;
import com.onseju.orderservice.stub.StubCompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class OrderServiceTest {

	OrderService orderService;
	StubCompanyRepository companyRepository = new StubCompanyRepository();
	FakeOrderRepository orderRepository = new FakeOrderRepository();
	OrderMapper orderMapper = new OrderMapper();
	OrderEventPublisher eventPublisher;
	MatchedEventPublisher matchedEventPublisher;
	UserServiceClient userServiceClient;
	TsidGenerator tsidGenerator;

	@BeforeEach
	void setUp() {
		eventPublisher = Mockito.mock(OrderEventPublisher.class);
		matchedEventPublisher = Mockito.mock(MatchedEventPublisher.class);
		userServiceClient = Mockito.mock(UserServiceClient.class);
		tsidGenerator = Mockito.mock(TsidGenerator.class);
		SimpMessagingTemplate messagingTemplate = Mockito.mock(SimpMessagingTemplate.class);
		orderService = new OrderService(
			orderRepository, companyRepository, eventPublisher, matchedEventPublisher, 
			userServiceClient, orderMapper, tsidGenerator, messagingTemplate);
	}

	@Nested
	class CreateOrderTest {

		@Test
		@DisplayName("TC20.2.1 주문 생성 테스트")
		void testPlaceOrder() {
			BeforeTradeOrderDto params = createBeforeTradeOrderDto("LIMIT_BUY", new BigDecimal(1), new BigDecimal(1000), 1L);
			when(userServiceClient.validateOrder(any()))
				.thenReturn(new OrderValidationResponse(1L, true));
			assertThatNoException().isThrownBy(() -> orderService.placeOrder(params));
		}

		@Test
		@DisplayName("주문 생성 성공 테스트")
		void placeOrderSuccess() {
			// given
			BeforeTradeOrderDto params = createBeforeTradeOrderDto("LIMIT_BUY", new BigDecimal(1), new BigDecimal(1000), 1L);
			when(userServiceClient.validateOrder(any()))
				.thenReturn(new OrderValidationResponse(1L, true));

			OrderResponse expected = new OrderResponse(
					1L,
					params.companyCode(),
					Type.valueOf(params.type()),
					params.totalQuantity(),
					params.price()
			);
			ApiResponse<OrderResponse> response = orderService.placeOrder(params);
			assertThat(response.getMessage()).isEqualTo("주문 접수 성공");
			assertThat(response.getData().companyCode()).isEqualTo(expected.companyCode());
			assertThat(response.getData().type()).isEqualTo(expected.type());
			assertThat(response.getData().totalQuantity()).isEqualTo(expected.totalQuantity());
			assertThat(response.getData().price()).isEqualTo(expected.price());
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
			BeforeTradeOrderDto params = createBeforeTradeOrderDto("LIMIT_BUY", new BigDecimal(1), price, 1L);
			when(userServiceClient.validateOrder(any()))
				.thenReturn(new OrderValidationResponse(1L, true));

			// when, then
			assertThatNoException().isThrownBy(() -> orderService.placeOrder(params));
		}

		@Test
		@DisplayName("입력된 가격이 종가 기준 상향 30%를 초과할 경우 예외가 발생한다.")
		void throwExceptionWhenPriceExceedsUpperLimit() {
			// given
			BigDecimal price = new BigDecimal(1301);
			BeforeTradeOrderDto params = createBeforeTradeOrderDto("LIMIT_SELL", new BigDecimal(10), price, 1L);

			// when, then
			assertThatThrownBy(() -> orderService.placeOrder(params)).isInstanceOf(PriceOutOfRangeException.class);
		}

		@Test
		@DisplayName("입력된 가격이 종가 기준 하향 30% 이하일 경우 정상적으로 처리한다.")
		void placeOrderWhenPriceWithinLowerLimit() {
			// given
			BigDecimal price = new BigDecimal(700);
			BeforeTradeOrderDto params = createBeforeTradeOrderDto("LIMIT_BUY", new BigDecimal(10), price, 1L);
			when(userServiceClient.validateOrder(any()))
				.thenReturn(new OrderValidationResponse(1L, true));

			// when, then
			assertThatNoException().isThrownBy(() -> orderService.placeOrder(params));
		}

		@Test
		@DisplayName("입력된 가격이 종가 기준 하향 30% 미만일 경우 예외가 발생한다.")
		void throwExceptionWhenPriceIsBelowLowerLimit() {
			// given
			BigDecimal price = new BigDecimal(699);
			BeforeTradeOrderDto params = createBeforeTradeOrderDto("LIMIT_BUY", new BigDecimal(10), price, 1L);

			// when, then
			assertThatThrownBy(() -> orderService.placeOrder(params))
				.isInstanceOf(PriceOutOfRangeException.class);
		}

		@Test
		@DisplayName("입력 가격이 음수일 경우 예외가 발생한다.")
		void throwExceptionWhenInvalidPrice() {
			// given
			BigDecimal price = new BigDecimal(-1);
			BeforeTradeOrderDto params = createBeforeTradeOrderDto("LIMIT_BUY", new BigDecimal(10), price, 1L);

			// when, then
			assertThatThrownBy(() -> orderService.placeOrder(params)).isInstanceOf(OrderPriceQuotationException.class);
		}

		@Test
		@DisplayName("유효하지 않은 단위의 가격이 입력될 경우 예외가 발생한다.")
		void throwExceptionWhenInvalidUnitPrice() {
			// given
			BigDecimal price = new BigDecimal("0.5");
			BeforeTradeOrderDto params = createBeforeTradeOrderDto("LIMIT_BUY", new BigDecimal(10), price, 1L);

			// when, then
			assertThatThrownBy(() -> orderService.placeOrder(params)).isInstanceOf(OrderPriceQuotationException.class);
		}
	}

	@Nested
	@DisplayName("user-service와의 통신 테스트")
	public class CommunicationWithUserService {

		@Test
		@DisplayName("외부 모듈과 통신을 실패할 경우 예외를 발생시킨다.")
		void communicationWithUserService() {
			// given
			BigDecimal price = new BigDecimal(1300);
			BeforeTradeOrderDto params = createBeforeTradeOrderDto("LIMIT_BUY", new BigDecimal(1), price, 1L);
			when(userServiceClient.validateOrder(any()))
				.thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

			// when, then
			assertThatThrownBy(() -> orderService.placeOrder(params))
				.isInstanceOf(HttpClientErrorException.class);
		}
	}

	private BeforeTradeOrderDto createBeforeTradeOrderDto(
		String type,
		BigDecimal totalQuantity,
		BigDecimal price,
		Long memberId
	) {
		return new BeforeTradeOrderDto(
			"005930",
			type,
			totalQuantity,
			price,
			memberId
		);
	}
}
