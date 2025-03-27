package com.onseju.orderservice.order.client;

import com.onseju.orderservice.grpc.GrpcValidateRequest;
import com.onseju.orderservice.grpc.GrpcValidateResponse;
import com.onseju.orderservice.grpc.OrderValidationServiceGrpc;
import com.onseju.orderservice.order.dto.BeforeTradeOrderDto;
import com.onseju.orderservice.order.dto.OrderValidationResponse;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceClientTest {

	private final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

	private OrderValidationServiceGrpc.OrderValidationServiceImplBase serviceImpl =
		mock(OrderValidationServiceGrpc.OrderValidationServiceImplBase.class);

	private UserServiceClient userServiceClient;

	@BeforeEach
	void setUp() throws IOException {
		// spy 생성
		serviceImpl = spy(new OrderValidationServiceGrpc.OrderValidationServiceImplBase() {});

		// 고유한 서버 이름 생성
		String serverName = InProcessServerBuilder.generateName();

		// InProcess 서버 등록 및 시작
		grpcCleanup.register(
			InProcessServerBuilder.forName(serverName)
				.directExecutor()
				.addService(serviceImpl)  // Add the spy service implementation
				.build()
				.start());

		// InProcess 채널 생성
		OrderValidationServiceGrpc.OrderValidationServiceBlockingStub blockingStub =
			OrderValidationServiceGrpc.newBlockingStub(
				grpcCleanup.register(
					InProcessChannelBuilder.forName(serverName)
						.directExecutor()
						.build()));

		// 테스트 대상 클라이언트 생성
		userServiceClient = new UserServiceClient(blockingStub);
	}


	@Test
	void validateOrder_Success() {
		// Given
		BeforeTradeOrderDto dto = BeforeTradeOrderDto.builder()
			.companyCode("AAPL")
			.type("LIMIT_BUY")
			.totalQuantity(new BigDecimal("10"))
			.price(new BigDecimal("150.5"))
			.memberId(123L)
			.build();

		// Mock 서비스 구현 설정
		doAnswer(invocation -> {
			StreamObserver<GrpcValidateResponse> responseObserver =
				(StreamObserver<GrpcValidateResponse>) invocation.getArguments()[1];

			GrpcValidateResponse response = GrpcValidateResponse.newBuilder()
				.setAccountId(456L)
				.setResult(true)
				.setMessage("검증 성공")
				.build();

			responseObserver.onNext(response);
			responseObserver.onCompleted();
			return null;
		}).when(serviceImpl).validateOrder(any(GrpcValidateRequest.class), any(StreamObserver.class));

		// When
		OrderValidationResponse response = userServiceClient.validateOrder(dto);

		// Then
		assertNotNull(response);
		assertEquals(456L, response.accountId());
		assertTrue(response.result());

		// 요청 검증
		ArgumentCaptor<GrpcValidateRequest> requestCaptor =
			ArgumentCaptor.forClass(GrpcValidateRequest.class);
		verify(serviceImpl).validateOrder(requestCaptor.capture(), any(StreamObserver.class));

		GrpcValidateRequest capturedRequest = requestCaptor.getValue();
		assertEquals("AAPL", capturedRequest.getCompanyCode());
		assertEquals("LIMIT_BUY", capturedRequest.getType());
		assertEquals("10", capturedRequest.getTotalQuantity());
		assertEquals("150.5", capturedRequest.getPrice());
		assertEquals(123L, capturedRequest.getMemberId());
	}

	@Test
	void validateOrder_Failure() {
		// Given
		BeforeTradeOrderDto dto = BeforeTradeOrderDto.builder()
			.companyCode("AAPL")
			.type("LIMIT_BUY")
			.totalQuantity(new BigDecimal("10"))
			.price(new BigDecimal("150.5"))
			.memberId(123L)
			.build();

		// Mock 서비스 구현 설정 - 실패 응답
		doAnswer(invocation -> {
			StreamObserver<GrpcValidateResponse> responseObserver =
				(StreamObserver<GrpcValidateResponse>) invocation.getArguments()[1];

			GrpcValidateResponse response = GrpcValidateResponse.newBuilder()
				.setAccountId(0L)
				.setResult(false)
				.setMessage("잔액 부족")
				.build();

			responseObserver.onNext(response);
			responseObserver.onCompleted();
			return null;
		}).when(serviceImpl).validateOrder(any(GrpcValidateRequest.class), any(StreamObserver.class));

		// When
		OrderValidationResponse response = userServiceClient.validateOrder(dto);

		// Then
		assertNotNull(response);
		assertEquals(0L, response.accountId());
		assertFalse(response.result());
	}

	@Test
	void validateOrder_Exception() {
		// Given
		BeforeTradeOrderDto dto = BeforeTradeOrderDto.builder()
			.companyCode("AAPL")
			.type("LIMIT_BUY")
			.totalQuantity(new BigDecimal("10"))
			.price(new BigDecimal("150.5"))
			.memberId(123L)
			.build();

		// Mock 서비스 구현 설정 - 예외 발생
		doAnswer(invocation -> {
			StreamObserver<GrpcValidateResponse> responseObserver =
				(StreamObserver<GrpcValidateResponse>) invocation.getArguments()[1];
			responseObserver.onError(new RuntimeException("서비스 내부 오류"));
			return null;
		}).when(serviceImpl).validateOrder(any(GrpcValidateRequest.class), any(StreamObserver.class));

		// When & Then
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			userServiceClient.validateOrder(dto);
		});

		assertEquals("gRPC 서비스 통신 오류", exception.getMessage());
	}
}
