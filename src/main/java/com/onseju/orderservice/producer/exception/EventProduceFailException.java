package com.onseju.orderservice.producer.exception;

import org.springframework.http.HttpStatus;

import com.onseju.orderservice.global.exception.BaseException;

public class EventProduceFailException extends BaseException {
	public EventProduceFailException(){
		super("이벤트 발행 실패", HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
