package com.onseju.orderservice.tradehistory.dto;

import java.math.BigDecimal;

public record TradeAvgPriceDto(String companyCode, BigDecimal avgPrice) {}

