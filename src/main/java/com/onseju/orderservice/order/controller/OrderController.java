package com.onseju.orderservice.order.controller;

import com.onseju.orderservice.global.security.UserDetailsImpl;
import com.onseju.orderservice.order.controller.request.OrderRequest;
import com.onseju.orderservice.order.service.OrderService;
import com.onseju.orderservice.order.service.dto.CreateOrderParams;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

	private final OrderService orderService;

	@PostMapping
	public ResponseEntity<Void> received(
			@RequestBody final OrderRequest request,
			@AuthenticationPrincipal final UserDetailsImpl user
	) {
		orderService.placeOrder(
			new CreateOrderParams(
					request.companyCode(),
					request.type(),
					request.totalQuantity(),
					request.price(),
					request.now(),
					user.getMember().getId()
			)
		);
		return ResponseEntity.ok().build();
	}
}
