package com.onseju.orderservice.order.client;

import org.springframework.stereotype.Service;

import net.devh.boot.grpc.client.inject.GrpcClient;

import com.onseju.orderservice.grpc.GrpcValidateResponse;
import com.onseju.orderservice.grpc.GrpcValidateRequest;
import com.onseju.orderservice.grpc.OrderValidationServiceGrpc;
import com.onseju.orderservice.order.dto.BeforeTradeOrderDto;
import com.onseju.orderservice.order.dto.OrderValidationResponse;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Service
public class UserServiceClient {

	@GrpcClient("order-service")
	private OrderValidationServiceGrpc.OrderValidationServiceBlockingStub orderValidationServiceBlockingStub;

	public OrderValidationResponse validateOrder(BeforeTradeOrderDto dto) {

		try {

			GrpcValidateRequest request = GrpcValidateRequest.newBuilder()
				.setCompanyCode(dto.companyCode())
				.setType(dto.type())
				.setTotalQuantity(dto.totalQuantity().toPlainString())
				.setPrice(dto.price().toPlainString())
				.setMemberId(dto.memberId())
				.build();

			GrpcValidateResponse response = orderValidationServiceBlockingStub.validateOrder(request);

			// gRPC 응답을 ValidateResponse 객체로 변환
			OrderValidationResponse validateResponse = OrderValidationResponse.builder()
				.accountId(response.getAccountId())
				.result(response.getResult())
				.build();

			return validateResponse;
		} catch (Exception e) {
			throw new RuntimeException("gRPC 서비스 통신 오류", e);
		}
	}



}
