package com.onseju.orderservice.order.service;

import com.onseju.orderservice.client.UserServiceClients;
import com.onseju.orderservice.client.dto.OrderValidationResponse;
import com.onseju.orderservice.company.domain.Company;
import com.onseju.orderservice.company.service.CompanyRepository;
import com.onseju.orderservice.order.domain.Order;
import com.onseju.orderservice.order.exception.PriceOutOfRangeException;
import com.onseju.orderservice.order.mapper.OrderMapper;
import com.onseju.orderservice.order.service.dto.CreateOrderParams;
import com.onseju.orderservice.order.service.repository.OrderRepository;
import com.onseju.orderservice.order.service.validator.OrderValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@RequiredArgsConstructor
@Service
public class OrderService {

	private final OrderRepository orderRepository;
	private final CompanyRepository companyRepository;
	private final OrderMapper orderMapper;
	private final UserServiceClients userServiceClients;
	private final RabbitTemplate rabbitTemplate;

	@Transactional
	public void placeOrder(final CreateOrderParams params) {
		// 지정가 주문 가격 견적 유효성 검증
		final BigDecimal price = params.price();
		final OrderValidator validator = OrderValidator.getUnitByPrice(price);
		validator.isValidPrice(price);

		// 종가 기준 검증
		validateClosingPrice(price, params.companyCode());

		// user-service와 rest 통신
		Long accountId = getAccountIdFromUserService(params);

		Order savedOrder = orderRepository.save(orderMapper.toEntity(params, accountId));
		rabbitTemplate.convertAndSend("ordered.exchange", "ordered.key", orderMapper.toEvent(savedOrder));
	}

	// 종가 기준 가격 검증
	private void validateClosingPrice(final BigDecimal price, final String companyCode) {
		final Company company = companyRepository.findByIsuSrtCd(companyCode);

		if (!company.isWithinClosingPriceRange(price)) {
			throw new PriceOutOfRangeException();
		}
	}

	// 외부의 user-service와 rest 통신
	private Long getAccountIdFromUserService(final CreateOrderParams params) {
		OrderValidationResponse clientsResponse = userServiceClients.validateOrderAndGetAccountId(
				orderMapper.toReservationOrderRequest(params)).getBody();
		return clientsResponse.accountId();
	}
}
