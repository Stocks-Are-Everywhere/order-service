package com.onseju.orderservice.order.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.onseju.orderservice.global.response.ApiResponse;
import com.onseju.orderservice.global.security.UserDetailsImpl;
import com.onseju.orderservice.order.controller.request.OrderRequest;
import com.onseju.orderservice.order.controller.resposne.OrderResponse;
import com.onseju.orderservice.order.dto.BeforeTradeOrderDto;
import com.onseju.orderservice.order.service.OrderService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

	private final OrderService orderService;

	@PostMapping
	public ResponseEntity<ApiResponse<OrderResponse>> received(
			@RequestBody final OrderRequest request,
			@AuthenticationPrincipal final UserDetailsImpl user
	) {
		final BeforeTradeOrderDto dto = BeforeTradeOrderDto.builder()
				.companyCode(request.companyCode())
				.type(request.type().name())
				.totalQuantity(request.totalQuantity())
				.price(request.price())
				.memberId(user.getMember().getId())
				.build();
		ApiResponse<OrderResponse> response = orderService.placeOrder(dto);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
}
