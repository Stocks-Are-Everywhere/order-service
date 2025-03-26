package com.onseju.orderservice.order.controller;

import com.onseju.orderservice.global.response.ApiResponse;
import com.onseju.orderservice.global.security.UserDetailsImpl;
import com.onseju.orderservice.order.controller.request.OrderRequest;
import com.onseju.orderservice.order.controller.response.OrderResponse;
import com.onseju.orderservice.order.service.OrderService;
import com.onseju.orderservice.order.service.dto.CreateOrderParams;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

	private final OrderService orderService;

	@PostMapping
	public ResponseEntity<ApiResponse<OrderResponse>> received(
			@RequestBody final OrderRequest request
			// @RequestHeader("Authorization") String authorizationHeader
	) {
		ApiResponse<OrderResponse> response = orderService.placeOrder(
			new CreateOrderParams(
					request.companyCode(),
					request.type(),
					request.totalQuantity(),
					request.price(),
					request.now(),
					// user.getMember().getId()
					request.memberId()
			)
		);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
}