package com.onseju.orderservice.order.service;

import com.onseju.orderservice.company.domain.Company;
import com.onseju.orderservice.company.service.CompanyRepository;
import com.onseju.orderservice.holding.domain.Holdings;
import com.onseju.orderservice.holding.service.HoldingsRepository;
import com.onseju.orderservice.order.client.OrderServiceClient;
import com.onseju.orderservice.order.client.OrderServiceGrpcClient;
import com.onseju.orderservice.order.client.response.ValidateResponse;
import com.onseju.orderservice.order.domain.Account;
import com.onseju.orderservice.order.domain.Order;
import com.onseju.orderservice.order.exception.PriceOutOfRangeException;
import com.onseju.orderservice.order.mapper.OrderMapper;
import com.onseju.orderservice.order.service.dto.CreateOrderParams;
import com.onseju.orderservice.order.service.dto.OrderedEvent;
import com.onseju.orderservice.order.service.repository.AccountRepository;
import com.onseju.orderservice.order.service.repository.OrderRepository;
import com.onseju.orderservice.order.service.validator.OrderValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@RequiredArgsConstructor
@Service
public class OrderService {

	private final OrderRepository orderRepository;
	private final CompanyRepository companyRepository;
	private final HoldingsRepository holdingsRepository;
	private final AccountRepository accountRepository;
	private final OrderMapper orderMapper;
	private final RabbitTemplate rabbitTemplate;
	// private final OrderServiceClient orderServiceClient;
	private final OrderServiceGrpcClient orderServiceClient;

	@Transactional
	public void placeOrder(final CreateOrderParams params) throws Exception {
		// 지정가 주문 가격 견적 유효성 검증
		log.info("place order");
		final BigDecimal price = params.price();
		final OrderValidator validator = OrderValidator.getUnitByPrice(price);
		validator.isValidPrice(price);

		// 종가 기준 검증
		validateClosingPrice(price, params.companyCode());

		ValidateResponse response = orderServiceClient.validateOrder(params);

		if (!response.valid()) {
			// TODO : 예외 추가
			throw new Exception();
		} else {
			log.info("완료");
			Order savedOrder = orderRepository.save(orderMapper.toEntity(params, response.accountId()));
			OrderedEvent event = orderMapper.toEvent(savedOrder);
			//체결 서비스로 이벤트 발행
			rabbitTemplate.convertAndSend("matching.exchange", "matching.event", event);
		}

	}

	// 종가 기준 가격 검증
	private void validateClosingPrice(final BigDecimal price, final String companyCode) {
		final Company company = companyRepository.findByIsuSrtCd(companyCode);

		if (!company.isWithinClosingPriceRange(price)) {
			throw new PriceOutOfRangeException();
		}
	}


}
