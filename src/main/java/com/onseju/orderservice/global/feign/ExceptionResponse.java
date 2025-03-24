package com.onseju.orderservice.global.feign;

public record ExceptionResponse(String message, String data, Integer statusCode) {
}
