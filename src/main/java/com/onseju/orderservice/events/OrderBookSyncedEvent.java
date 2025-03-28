package com.onseju.orderservice.events;

import java.util.List;
import java.util.UUID;

import com.onseju.orderservice.order.dto.PriceLevelDto;

import lombok.Builder;

@Builder
public record OrderBookSyncedEvent(
		UUID id,
		String companyCode,
		List<PriceLevelDto> sellLevels,
		List<PriceLevelDto> buyLevels
) {
}
