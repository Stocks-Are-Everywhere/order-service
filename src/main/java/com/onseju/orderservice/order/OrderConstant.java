package com.onseju.orderservice.order;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum OrderConstant {

    CLOSING_PRICE_LIMIT(30);

    private final Integer value;
}
