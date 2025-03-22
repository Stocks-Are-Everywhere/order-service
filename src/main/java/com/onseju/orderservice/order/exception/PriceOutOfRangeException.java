package com.onseju.orderservice.order.exception;

import com.onseju.orderservice.global.exception.BaseException;
import org.springframework.http.HttpStatus;

public class PriceOutOfRangeException extends BaseException {

    public PriceOutOfRangeException() {
        super("주문 가격은 종가의 30% 범위 내에서만 설정할 수 있습니다", HttpStatus.BAD_REQUEST);
    }
}
