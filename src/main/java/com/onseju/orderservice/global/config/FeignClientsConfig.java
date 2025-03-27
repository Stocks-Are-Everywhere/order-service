package com.onseju.orderservice.global.config;

import com.onseju.orderservice.global.feign.FeignErrorDecoder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.onseju.orderservice.order.client")
public class FeignClientsConfig {

    @Bean
    public FeignErrorDecoder feignErrorDecoder() {
        return new FeignErrorDecoder();
    }
}
