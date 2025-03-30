package com.onseju.orderservice.tradehistory.dto;

import java.math.BigDecimal;

public record TotalTradeAmountDto(String companyCode, String companyName, BigDecimal totalAmount) {}

