package com.onseju.orderservice.events.exception;

import org.springframework.http.HttpStatus;

import com.onseju.orderservice.global.exception.BaseException;


public class OrderEventPublisherFailException extends BaseException {

    public OrderEventPublisherFailException() {
        super("ORDER SERVICE - 주문 이벤트 발행 실패", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
