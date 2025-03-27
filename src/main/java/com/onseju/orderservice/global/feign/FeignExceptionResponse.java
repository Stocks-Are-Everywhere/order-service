package com.onseju.orderservice.global.feign;

public record FeignExceptionResponse(String message, String data, Integer statusCode) {
}

