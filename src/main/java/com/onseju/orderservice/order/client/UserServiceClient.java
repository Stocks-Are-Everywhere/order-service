package com.onseju.orderservice.order.client;

import com.onseju.orderservice.order.dto.BeforeTradeOrderDto;
import com.onseju.orderservice.order.dto.OrderValidationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service", url = "${user-service.url}")
public interface UserServiceClient {

	@PostMapping("/api/user-service/validate")
	ResponseEntity<OrderValidationResponse> validateOrderAndGetAccountId(@RequestBody BeforeTradeOrderDto request);
}
