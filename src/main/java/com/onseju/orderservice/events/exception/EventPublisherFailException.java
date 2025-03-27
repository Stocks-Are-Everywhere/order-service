package com.onseju.orderservice.events.exception;

import org.springframework.http.HttpStatus;

import com.onseju.orderservice.global.exception.BaseException;


public class EventPublisherFailException extends BaseException {

    public EventPublisherFailException() {
        super("ORDER SERVICE - 이벤트 발행 실패", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}