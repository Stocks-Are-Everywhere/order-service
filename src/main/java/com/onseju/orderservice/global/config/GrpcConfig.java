package com.onseju.orderservice.global.config;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.devh.boot.grpc.client.channelfactory.GrpcChannelFactory;

import com.onseju.orderservice.grpc.OrderValidationServiceGrpc;

@Configuration
@ImportAutoConfiguration({
	net.devh.boot.grpc.client.autoconfigure.GrpcClientAutoConfiguration.class,
	net.devh.boot.grpc.client.autoconfigure.GrpcClientMetricAutoConfiguration.class,
	net.devh.boot.grpc.client.autoconfigure.GrpcClientHealthAutoConfiguration.class,
	net.devh.boot.grpc.client.autoconfigure.GrpcClientSecurityAutoConfiguration.class,
	net.devh.boot.grpc.client.autoconfigure.GrpcDiscoveryClientAutoConfiguration.class,
	net.devh.boot.grpc.common.autoconfigure.GrpcCommonCodecAutoConfiguration.class,
})
public class GrpcConfig {

	@Bean
	public OrderValidationServiceGrpc.OrderValidationServiceBlockingStub orderValidationStub(
		GrpcChannelFactory grpcChannelFactory) {
		return OrderValidationServiceGrpc.newBlockingStub(
			grpcChannelFactory.createChannel("user-service"));
	}
}

