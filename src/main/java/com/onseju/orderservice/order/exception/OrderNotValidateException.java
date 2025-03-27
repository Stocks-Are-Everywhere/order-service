package com.onseju.orderservice.order.exception;

import org.springframework.http.HttpStatus;

import com.onseju.orderservice.global.exception.BaseException;

public class OrderNotValidateException extends BaseException {
	public OrderNotValidateException() {
		super("유효하지 않은 주문입니다.", HttpStatus.BAD_REQUEST);
	}
}
