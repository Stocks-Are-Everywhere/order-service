package com.onseju.orderservice.chart.dto;

import lombok.Builder;

@Builder
public record ChartUpdateDto(
		Double price,
		Integer volume,
		String timeCode
) {
}
