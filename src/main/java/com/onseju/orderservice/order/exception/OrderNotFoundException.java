package com.onseju.orderservice.order.exception;

import com.onseju.orderservice.global.exception.BaseException;
import org.springframework.http.HttpStatus;

public class OrderNotFoundException extends BaseException {

    public OrderNotFoundException() {
        super("주문 내역이 존재하지 않습니다", HttpStatus.NOT_FOUND);
    }
}
