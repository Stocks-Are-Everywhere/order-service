package com.onseju.orderservice.order.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onseju.orderservice.company.service.ClosingPriceService;
import com.onseju.orderservice.company.service.repository.CompanyRepository;
import com.onseju.orderservice.events.MatchedEvent;
import com.onseju.orderservice.events.OrderBookSyncedEvent;
import com.onseju.orderservice.events.OrderCreatedEvent;
import com.onseju.orderservice.events.publisher.EventPublisher;
import com.onseju.orderservice.global.response.ApiResponse;
import com.onseju.orderservice.global.utils.TsidGenerator;
import com.onseju.orderservice.order.OrderConstant;
import com.onseju.orderservice.order.client.UserServiceClient;
import com.onseju.orderservice.order.controller.resposne.OrderResponse;
import com.onseju.orderservice.order.domain.Order;
import com.onseju.orderservice.order.dto.AfterTradeOrderDto;
import com.onseju.orderservice.order.dto.BeforeTradeOrderDto;
import com.onseju.orderservice.order.dto.OrderValidationResponse;
import com.onseju.orderservice.order.exception.OrderNotValidateException;
import com.onseju.orderservice.order.exception.PriceOutOfRangeException;
import com.onseju.orderservice.order.mapper.OrderMapper;
import com.onseju.orderservice.order.service.repository.OrderRepository;
import com.onseju.orderservice.order.service.validator.OrderValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class OrderService {

	private final OrderRepository orderRepository;
	private final CompanyRepository companyRepository;
	private final EventPublisher<OrderCreatedEvent> orderEventPublisher;
	private final EventPublisher<MatchedEvent> matchedEventPublisher;
	private final UserServiceClient userServiceClient;
	private final OrderMapper orderMapper;
	private final TsidGenerator tsidGenerator;
	private final ClosingPriceService closingPriceService;

	private final SimpMessagingTemplate messagingTemplate;

	@Transactional
	public ApiResponse<OrderResponse> placeOrder(final BeforeTradeOrderDto dto) {
		// 주문 유효성 검증
		validateOrder(dto.price(), dto.companyCode());

		// 계좌 및 보유 주식 검증(REST 요청)
		Long accountId = getAccountIdFromUserService(dto);

		// 주문 생성 이벤트 발행
		final Long orderId = tsidGenerator.nextId();
		final Order order = orderMapper.toEntity(orderId, dto, accountId);
		final OrderCreatedEvent event = orderMapper.toEvent(order);
		orderEventPublisher.publishEvent(event);

		OrderResponse orderResponse = OrderResponse.builder()
				.id(order.getId())
				.companyCode(order.getCompanyCode())
				.type(order.getType())
				.totalQuantity(order.getTotalQuantity())
				.price(order.getPrice())
				.build();

		return new ApiResponse<>(
				"주문 접수 성공",
				orderResponse,
				HttpStatus.OK.value());
	}

	/**
	 * 주문 유효성 검증
	 */
	private void validateOrder(final BigDecimal price, final String companyCode) {
		// 호가 단위 검증
		OrderValidator validator = OrderValidator.getUnitByPrice(price);
		validator.isValidPrice(price);

		// 전날 종가 검증
		final BigDecimal closingPrice = closingPriceService.getClosingPrice(companyCode);

		if (!isWithinClosingPriceRange(closingPrice, price)) {
			throw new PriceOutOfRangeException();
		}
	}

	private boolean isWithinClosingPriceRange(final BigDecimal closingPrice, final BigDecimal price) {
		final BigDecimal percentageDivisor = new BigDecimal(100);
		final BigDecimal priceLimit = BigDecimal.valueOf(OrderConstant.CLOSING_PRICE_LIMIT.getValue());

		final BigDecimal lowerBound = calculatePriceLimit(closingPrice, percentageDivisor, priceLimit.negate());
		final BigDecimal upperBound = calculatePriceLimit(closingPrice, percentageDivisor, priceLimit);

		return price.compareTo(lowerBound) >= 0 && price.compareTo(upperBound) <= 0;
	}

	private BigDecimal calculatePriceLimit(
			final BigDecimal closingPrice,
			final BigDecimal percentageDivisor,
			final BigDecimal priceLimit
	) {
		return closingPrice.multiply(new BigDecimal(100).add(priceLimit))
				.divide(percentageDivisor, RoundingMode.HALF_UP);
	}

	// 외부의 user-service와 rest 통신
	private Long getAccountIdFromUserService(final BeforeTradeOrderDto dto) {
		OrderValidationResponse clientsResponse = userServiceClient.validateOrder(dto);

		// 검증 결과 확인
		if (!clientsResponse.result()) {
			throw new OrderNotValidateException();
		}

		return clientsResponse.accountId();
	}

	/**
	 * 주문 예약 수량 업데이트
	 */
	@Transactional
	public void updateRemainingQuantity(final AfterTradeOrderDto dto) {
		final Order order = orderRepository.getById(dto.orderId());
		order.decreaseRemainingQuantity(dto.quantity());
		orderRepository.save(order);
	}

	/**
	 * 주문장 업데이트 (매칭 엔진으로부터 수신)
	 */
	public void broadcastOrderBookUpdate(final OrderBookSyncedEvent event) {
		messagingTemplate.convertAndSend("/topic/orderbook/" + event.companyCode(), event);
	}

	/**
	 * 매칭 후, 사용자 업데이트 이벤트 발행
	 */
	public void publishUserUpdateEvent(final MatchedEvent event) {
		matchedEventPublisher.publishEvent(event);
	}

	@Transactional
	public void saveOrder(final Order order) {
		orderRepository.save(order);
	}
}
