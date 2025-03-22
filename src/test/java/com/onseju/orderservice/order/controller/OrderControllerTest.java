package com.onseju.orderservice.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onseju.orderservice.global.jwt.JwtUtil;
import com.onseju.orderservice.global.security.UserDetailsServiceImpl;
import com.onseju.orderservice.mock.WithMockUserDetails;
import com.onseju.orderservice.order.controller.request.OrderRequest;
import com.onseju.orderservice.order.domain.Type;
import com.onseju.orderservice.order.service.OrderService;
import com.onseju.orderservice.order.service.dto.CreateOrderParams;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OrderService orderService;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private JwtUtil jwtUtil;

	@MockitoBean
	UserDetailsServiceImpl userDetailsServiceImpl;

	@Test
	@DisplayName("주문 생성 테스트")
	@WithMockUserDetails
	void testReceived() throws Exception {
		OrderRequest request = OrderRequest.builder()
				.companyCode("AAPL")
				.type(Type.LIMIT_BUY)
				.totalQuantity(new BigDecimal("10"))
				.price(new BigDecimal("150.00"))
				.build();

		mockMvc.perform(post("/api/order")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk());
		verify(orderService).placeOrder(any(CreateOrderParams.class));
	}
}
