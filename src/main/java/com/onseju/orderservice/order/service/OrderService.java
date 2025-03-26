package com.onseju.orderservice.order.service;

import com.onseju.orderservice.company.domain.Company;
import com.onseju.orderservice.company.service.CompanyRepository;
import com.onseju.orderservice.global.response.ApiResponse;
import com.onseju.orderservice.global.utils.TsidGenerator;
import com.onseju.orderservice.holding.domain.Holdings;
import com.onseju.orderservice.holding.service.HoldingsRepository;
import com.onseju.orderservice.order.controller.response.OrderResponse;
import com.onseju.orderservice.order.domain.Account;
import com.onseju.orderservice.order.exception.PriceOutOfRangeException;
import com.onseju.orderservice.order.mapper.OrderMapper;
import com.onseju.orderservice.order.service.dto.CreateOrderParams;
import com.onseju.orderservice.order.service.dto.OrderedEvent;
import com.onseju.orderservice.order.service.repository.AccountRepository;
import com.onseju.orderservice.order.service.repository.OrderRepository;
import com.onseju.orderservice.producer.EventProducer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
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
	private final EventProducer<OrderedEvent> eventProducer;
	private final TsidGenerator tsidGenerator;

	@Transactional
	public ApiResponse<OrderResponse> placeOrder(final CreateOrderParams params) {
		// 지정가 주문 가격 견적 유효성 검증
		// final BigDecimal price = params.price();
		// final OrderValidator validator = OrderValidator.getUnitByPrice(price);
		// validator.isValidPrice(price);
		//
		// // 종가 기준 검증
		// // TODO: 인터널콜 추가
		// validateClosingPrice(price, params.companyCode());
		//
		// Account account = accountRepository.getByMemberId(params.memberId());
		// validateAccount(params, account);
		// validateHoldings(account.getId(), params);

		Long orderid = tsidGenerator.nextId();
		OrderedEvent orderedEvent = orderMapper.toOrderCreateEvent(orderid, params, params.memberId());
		eventProducer.publishEvent(orderedEvent);

		OrderResponse orderResponse = OrderResponse.builder()
				.id(orderid)
				.companyCode(params.companyCode())
				.type(params.type())
				.build();

		return new ApiResponse<>(
				"주문생성 성공",
				orderResponse,
				HttpStatus.OK.value());
	}

	// 종가 기준 가격 검증
	private void validateClosingPrice(final BigDecimal price, final String companyCode) {
		final Company company = companyRepository.findByIsuSrtCd(companyCode);

		if (!company.isWithinClosingPriceRange(price)) {
			throw new PriceOutOfRangeException();
		}
	}

	private void validateAccount(final CreateOrderParams params, final Account account) {
		if (params.type().isBuy()) {
			account.validateDepositBalance(params.price().multiply(params.totalQuantity()));
		}
	}

	private void validateHoldings(final Long accountId, final CreateOrderParams params) {
		if (params.type().isSell()) {
			final Holdings holdings = holdingsRepository.getByAccountIdAndCompanyCode(accountId, params.companyCode());
			holdings.validateExistHoldings();
			holdings.validateEnoughHoldings(params.totalQuantity());
		}
	}
}
