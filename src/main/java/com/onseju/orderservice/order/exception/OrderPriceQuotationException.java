package com.onseju.orderservice.order.exception;

import com.onseju.orderservice.global.exception.BaseException;
import org.springframework.http.HttpStatus;

public class OrderPriceQuotationException extends BaseException {
	public OrderPriceQuotationException(final String message) {
		super(message, HttpStatus.BAD_REQUEST);
	}
}
