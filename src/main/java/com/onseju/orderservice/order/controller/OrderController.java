package com.onseju.orderservice.order.controller;

import com.onseju.orderservice.global.security.UserDetailsImpl;
import com.onseju.orderservice.order.controller.request.OrderRequest;
import com.onseju.orderservice.order.dto.BeforeTradeOrderDto;
import com.onseju.orderservice.order.service.OrderService;
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
		final BeforeTradeOrderDto dto = BeforeTradeOrderDto.builder()
				.companyCode(request.companyCode())
				.type(request.type())
				.totalQuantity(request.totalQuantity())
				.price(request.price())
				.memberId(user.getMember().getId())
				.build();
		orderService.placeOrder(dto);
		return ResponseEntity.ok().build();
	}
}
