package com.onseju.orderservice.order.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onseju.orderservice.grpc.GrpcValidateRequest;
import com.onseju.orderservice.grpc.GrpcValidateResponse;
import com.onseju.orderservice.grpc.OrderType;
import com.onseju.orderservice.order.client.response.ValidateResponse;
import com.onseju.orderservice.order.domain.Type;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@SpringBootTest
public class OrderServiceGrpcClientTest {

	private static final ObjectMapper objectMapper = new ObjectMapper();
	private static final int ITERATIONS = 1_000_000;
	private static final int WARMUP_ITERATIONS = 100_000;

	@MockBean
	RestTemplate restTemplate;

	@Test
	public void compareJsonVsProtobufPerformance() throws Exception {
		// JSON 객체 생성
		ValidateResponse jsonResponse = new ValidateResponse(
			true,
			"주문이 유효합니다",
			123456L,
			Type.LIMIT_BUY
		);

		// Protobuf 객체 생성
		GrpcValidateRequest protoRequest = GrpcValidateRequest.newBuilder()
			.setCompanyCode("AAPL")
			.setType(OrderType.LIMIT_BUY)
			.setTotalQuantity("10")
			.setPrice("150.00")
			.setNow(LocalDateTime.now().toString())
			.setMemberId(123456L)
			.build();

		GrpcValidateResponse protoResponse = GrpcValidateResponse.newBuilder()
			.setIsValid(true)
			.setMessage("주문이 유효합니다")
			.setAccountId(123456L)
			.setType(OrderType.LIMIT_BUY)
			.build();

		// 웜업
		System.out.println("웜업 시작...");
		for (int i = 0; i < WARMUP_ITERATIONS; i++) {
			// JSON 웜업
			byte[] jsonBytes = objectMapper.writeValueAsBytes(jsonResponse);
			objectMapper.readValue(jsonBytes, ValidateResponse.class);

			// Protobuf 웜업
			byte[] requestProtoBytes = protoRequest.toByteArray();
			GrpcValidateRequest.parseFrom(requestProtoBytes);

			byte[] responseProtoBytes = protoResponse.toByteArray();
			GrpcValidateResponse.parseFrom(responseProtoBytes);
		}
		System.out.println("웜업 완료");

		// JSON 직렬화/역직렬화 테스트
		long jsonStartTime = System.nanoTime();
		for (int i = 0; i < ITERATIONS; i++) {
			byte[] jsonBytes = objectMapper.writeValueAsBytes(jsonResponse);
			objectMapper.readValue(jsonBytes, ValidateResponse.class);
		}
		long jsonEndTime = System.nanoTime();
		long jsonDuration = (jsonEndTime - jsonStartTime) / 1_000_000; // 밀리초 단위로 변환

		// Protobuf 직렬화/역직렬화 테스트 (Request)
		long protoRequestStartTime = System.nanoTime();
		for (int i = 0; i < ITERATIONS; i++) {
			byte[] protoBytes = protoRequest.toByteArray();
			GrpcValidateRequest.parseFrom(protoBytes);
		}
		long protoRequestEndTime = System.nanoTime();
		long protoRequestDuration = (protoRequestEndTime - protoRequestStartTime) / 1_000_000;

		// Protobuf 직렬화/역직렬화 테스트 (Response)
		long protoResponseStartTime = System.nanoTime();
		for (int i = 0; i < ITERATIONS; i++) {
			byte[] protoBytes = protoResponse.toByteArray();
			GrpcValidateResponse.parseFrom(protoBytes);
		}
		long protoResponseEndTime = System.nanoTime();
		long protoResponseDuration = (protoResponseEndTime - protoResponseStartTime) / 1_000_000;

		// 크기 비교
		byte[] jsonBytes = objectMapper.writeValueAsBytes(jsonResponse);
		byte[] requestProtoBytes = protoRequest.toByteArray();
		byte[] responseProtoBytes = protoResponse.toByteArray();

		// 결과 출력
		System.out.println("===== 성능 테스트 결과 =====");
		System.out.println("반복 횟수: " + ITERATIONS);
		System.out.println();

		System.out.println("JSON 직렬화/역직렬화 시간: " + jsonDuration + " ms");
		System.out.println("Protobuf Request 직렬화/역직렬화 시간: " + protoRequestDuration + " ms");
		System.out.println("Protobuf Response 직렬화/역직렬화 시간: " + protoResponseDuration + " ms");
		System.out.println();

		System.out.println("JSON 바이트 크기: " + jsonBytes.length + " bytes");
		System.out.println("Protobuf Request 바이트 크기: " + requestProtoBytes.length + " bytes");
		System.out.println("Protobuf Response 바이트 크기: " + responseProtoBytes.length + " bytes");
		System.out.println();

		System.out.println("JSON 대비 Protobuf Request 속도 향상: " +
			String.format("%.2f", (double)jsonDuration / protoRequestDuration) + "x");
		System.out.println("JSON 대비 Protobuf Response 속도 향상: " +
			String.format("%.2f", (double)jsonDuration / protoResponseDuration) + "x");
		System.out.println("JSON 대비 Protobuf Request 크기 감소: " +
			String.format("%.2f", (double)jsonBytes.length / requestProtoBytes.length) + "x");
		System.out.println("JSON 대비 Protobuf Response 크기 감소: " +
			String.format("%.2f", (double)jsonBytes.length / responseProtoBytes.length) + "x");
	}
}
