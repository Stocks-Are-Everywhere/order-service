package com.onseju.orderservice.order.service;

import com.onseju.orderservice.company.domain.Company;
import com.onseju.orderservice.company.service.repository.CompanyRepository;
import com.onseju.orderservice.events.CreatedEvent;
import com.onseju.orderservice.events.MatchedEvent;
import com.onseju.orderservice.events.OrderBookSyncedEvent;
import com.onseju.orderservice.events.UpdateEvent;
import com.onseju.orderservice.events.publisher.OrderEventPublisher;
import com.onseju.orderservice.order.client.UserServiceClient;
import com.onseju.orderservice.order.domain.Order;
import com.onseju.orderservice.order.dto.AfterTradeOrderDto;
import com.onseju.orderservice.order.dto.BeforeTradeOrderDto;
import com.onseju.orderservice.order.dto.OrderValidationResponse;
import com.onseju.orderservice.order.exception.PriceOutOfRangeException;
import com.onseju.orderservice.order.mapper.OrderMapper;
import com.onseju.orderservice.order.service.repository.OrderRepository;
import com.onseju.orderservice.order.service.validator.OrderValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@RequiredArgsConstructor
@Service
public class OrderService {

	private final OrderRepository orderRepository;
	private final CompanyRepository companyRepository;
	private final OrderEventPublisher eventPublisher;
	private final UserServiceClient userServiceClient;
	private final OrderMapper orderMapper;

	private final SimpMessagingTemplate messagingTemplate;

	@Transactional
	public void placeOrder(final BeforeTradeOrderDto dto) {
		// 주문 유효성 검증
		validateOrder(dto.price(), dto.companyCode());

		// 계좌 및 보유 주식 검증(REST 요청)
		Long accountId = getAccountIdFromUserService(dto);

		// 주문 저장
		final Order order = orderMapper.toEntity(dto, accountId);
		final Order savedOrder = orderRepository.save(order);

		// 주문 생성 이벤트 발행
		final CreatedEvent event = orderMapper.toEvent(savedOrder);
		eventPublisher.publishOrderCreated(event);
	}

	/**
	 * 주문 유효성 검증
	 */
	private void validateOrder(final BigDecimal price, final String companyCode) {
		// 호가 단위 검증
		OrderValidator validator = OrderValidator.getUnitByPrice(price);
		validator.isValidPrice(price);

		// 전날 종가 검증
		final Company company = companyRepository.findByIsuSrtCd(companyCode);

		if (!company.isWithinClosingPriceRange(price)) {
			throw new PriceOutOfRangeException();
		}
	}

	// 외부의 user-service와 rest 통신
	private Long getAccountIdFromUserService(final BeforeTradeOrderDto dto) {
		OrderValidationResponse clientsResponse = userServiceClient.validateOrderAndGetAccountId(dto).getBody();
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
		final UpdateEvent updateEvent = UpdateEvent.builder()
				.companyCode(event.companyCode())
				.buyOrderId(event.buyOrderId())
				.buyAccountId(event.buyAccountId())
				.sellOrderId(event.sellOrderId())
				.sellAccountId(event.sellAccountId())
				.quantity(event.quantity())
				.price(event.price())
				.tradeAt(event.tradeAt())
				.build();

		eventPublisher.publishUserUpdate(updateEvent);
	}
}
