package com.onseju.orderservice.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Getter
@Configuration
@ConfigurationProperties(prefix = "services.user-service")
public class ServiceProperties {
	private String url;
}

