package com.onseju.orderservice.order.client;

import org.springframework.stereotype.Service;

import net.devh.boot.grpc.client.inject.GrpcClient;

import com.onseju.orderservice.grpc.GrpcValidateRequest;
import com.onseju.orderservice.grpc.GrpcValidateResponse;
import com.onseju.orderservice.grpc.OrderType;
import com.onseju.orderservice.grpc.OrderValidationServiceGrpc;
import com.onseju.orderservice.order.client.response.ValidateResponse;
import com.onseju.orderservice.order.domain.Type;
import com.onseju.orderservice.order.service.dto.CreateOrderParams;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
public class OrderServiceGrpcClient {

	@GrpcClient("order-service")
	private OrderValidationServiceGrpc.OrderValidationServiceBlockingStub orderValidationServiceBlockingStub;

	public ValidateResponse validateOrder(CreateOrderParams params) {
		log.info("gRPC 클라이언트 - validateOrder 호출 시작: {}", params);

		try {
			GrpcValidateRequest request = GrpcValidateRequest.newBuilder()
				.setCompanyCode(params.companyCode())
				.setType(convertToGrpcOrderType(params.type()))
				.setTotalQuantity(params.totalQuantity().toPlainString())
				.setPrice(params.price().toPlainString())
				.setNow(params.now().toString())  // ISO-8601 형식으로 변환
				.setMemberId(params.memberId())
				.build();

			log.info("gRPC 요청 생성 완료: {}", request);


			GrpcValidateResponse response = orderValidationServiceBlockingStub.validateOrder(request);


			// gRPC 응답을 ValidateResponse 객체로 변환
			ValidateResponse validateResponse = convertToValidateResponse(response);

			if (validateResponse.valid()) {
				log.info("주문 유효성 검증 성공: accountId={}, type={}", validateResponse.accountId(), validateResponse.type());
			} else {
				log.warn("주문 유효성 검증 실패: {}", response.getMessage());
			}

			return validateResponse;
		} catch (Exception e) {
			log.error("gRPC 호출 중 오류 발생: {}", e.getMessage(), e);
			throw new RuntimeException("gRPC 서비스 통신 오류", e);
		}
	}

	// gRPC 응답을 ValidateResponse 객체로 변환하는 메서드
	private ValidateResponse convertToValidateResponse(GrpcValidateResponse grpcResponse) {
		return new ValidateResponse(
			grpcResponse.getIsValid(),
			grpcResponse.getMessage(),
			grpcResponse.getAccountId(),
			convertToJavaType(grpcResponse.getType())
		);
	}

	// gRPC OrderType을 Java Type으로 변환하는 메서드
	private Type convertToJavaType(OrderType grpcType) {
		return switch (grpcType) {
			case LIMIT_BUY -> Type.LIMIT_BUY;
			case LIMIT_SELL -> Type.LIMIT_SELL;
			case MARKET_BUY -> Type.MARKET_BUY;
			case MARKET_SELL -> Type.MARKET_SELL;
			default -> throw new IllegalArgumentException("지원하지 않는 주문 유형: " + grpcType);
		};
	}

	private OrderType convertToGrpcOrderType(Type type) {
		log.debug("Java enum Type을 gRPC OrderType으로 변환: {}", type);
		return switch (type) {
			case LIMIT_BUY -> OrderType.LIMIT_BUY;
			case LIMIT_SELL -> OrderType.LIMIT_SELL;
			case MARKET_BUY -> OrderType.MARKET_BUY;
			case MARKET_SELL -> OrderType.MARKET_SELL;
			default -> OrderType.UNKNOWN;
		};
	}
}


