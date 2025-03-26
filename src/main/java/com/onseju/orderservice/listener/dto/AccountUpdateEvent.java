package com.onseju.orderservice.listener.dto;


import java.math.BigDecimal;
import java.util.UUID;



public record AccountUpdateEvent(
		UUID id,
		Long accountId,
		BigDecimal balance,
		BigDecimal reservedBalance

) {
}

