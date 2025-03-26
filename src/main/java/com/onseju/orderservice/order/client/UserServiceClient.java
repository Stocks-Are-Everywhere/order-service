package com.onseju.orderservice.order.client;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.onseju.orderservice.order.dto.BeforeTradeOrderDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceClient {

	private final RestTemplate restTemplate;

	private final String userServiceUrl = "http://localhost:8083";

	public void validateAccountAndHoldings(final BeforeTradeOrderDto dto) {
		final String url = userServiceUrl + "/api/user-service/validate";
		Map<String, Object> payload = new HashMap<>();
		payload.put("companyCode", dto.companyCode());
		payload.put("type", dto.type());
		payload.put("totalQuantity", dto.totalQuantity());
		payload.put("price", dto.price());
		payload.put("accountId", dto.accountId());

		HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload);
		restTemplate.postForEntity(url, request, Void.class);
	}
}
