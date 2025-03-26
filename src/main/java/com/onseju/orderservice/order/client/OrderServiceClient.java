package com.onseju.orderservice.order.client;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.onseju.orderservice.global.config.ServiceProperties;
import com.onseju.orderservice.order.client.request.ValidateRequest;
import com.onseju.orderservice.order.client.response.ValidateResponse;
import com.onseju.orderservice.order.service.dto.CreateOrderParams;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OrderServiceClient {

	private final RestTemplate restTemplate;
	private final ServiceProperties serviceProperties;

	public OrderServiceClient(RestTemplate restTemplate,
		ServiceProperties serviceProperties) {
		this.restTemplate = restTemplate;
		this.serviceProperties = serviceProperties;
	}

	public ValidateResponse validateOrder(CreateOrderParams params) {
		log.info("client.validateorder ");
		// 요청 객체 생성
		ValidateRequest request = new ValidateRequest(
			params.companyCode(),
			params.type(),
			params.totalQuantity(),
			params.price(),
			params.now(),
			params.memberId()
		);

		try {
			// 외부 서비스 호출
			log.info("Calling user service at URL: {}", serviceProperties.getUrl() + "/api/account/validation");
			ResponseEntity<ValidateResponse> response = restTemplate.postForEntity(
				serviceProperties.getUrl()  + "/api/account/validation", request, ValidateResponse.class);

			log.info("Response received: {}", response);

			if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
				return response.getBody();
			} else {
				log.error("Invalid response: {}", response);
				return new ValidateResponse(false, "유효하지 않은 응답", null,
					response.getBody() != null ? response.getBody().type() : null);
			}
		} catch (RestClientException e) {
			log.error("RestClientException occurred: ", e);
			return new ValidateResponse(false, "서비스 통신 오류: " + e.getMessage(), null, null);
		}

	}

}

