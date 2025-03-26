package com.onseju.orderservice.events;

import java.util.List;

import com.onseju.orderservice.order.dto.PriceLevelDto;

import lombok.Builder;

@Builder
public record OrderBookSyncedEvent(
		String companyCode,
		List<PriceLevelDto> sellLevels,
		List<PriceLevelDto> buyLevels
) {
}
