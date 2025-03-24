package com.onseju.orderservice.client;

import com.onseju.orderservice.client.dto.OrderValidationResponse;
import com.onseju.orderservice.client.dto.ReservationOrderRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service", url = "${user-service.url}")
public interface UserServiceClients {

    @PostMapping("/api/members/order-reservation")
    ResponseEntity<OrderValidationResponse>     validateOrderAndGetAccountId(@RequestBody ReservationOrderRequest request);
}
