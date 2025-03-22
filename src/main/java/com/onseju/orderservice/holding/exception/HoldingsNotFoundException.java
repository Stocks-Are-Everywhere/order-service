package com.onseju.orderservice.holding.exception;

import com.onseju.orderservice.global.exception.BaseException;
import org.springframework.http.HttpStatus;

public class HoldingsNotFoundException extends BaseException {

    public HoldingsNotFoundException() {
        super("보유 주식이 없습니다.", HttpStatus.NOT_FOUND);
    }
}
