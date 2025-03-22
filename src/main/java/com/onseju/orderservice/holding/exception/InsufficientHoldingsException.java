package com.onseju.orderservice.holding.exception;

import com.onseju.orderservice.global.exception.BaseException;
import org.springframework.http.HttpStatus;

public class InsufficientHoldingsException extends BaseException {

    public InsufficientHoldingsException() {
        super("판매 가능한 보유 주식 수량이 부족합니다.", HttpStatus.BAD_REQUEST);
    }
}
