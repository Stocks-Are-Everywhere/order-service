package com.onseju.orderservice.order.mapper;

import com.onseju.orderservice.order.domain.Order;
import com.onseju.orderservice.order.domain.Type;
import com.onseju.orderservice.order.dto.BeforeTradeOrderDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMapperTest {

	private final OrderMapper orderMapper = new OrderMapper();

	@Test
	@DisplayName("OrderRequest를 Entity로 변환한다.")
	void toEntity() {
		// given
		String companyCode = "005930";
		BeforeTradeOrderDto beforeTradeOrderDto = new BeforeTradeOrderDto("005930", Type.LIMIT_BUY, new BigDecimal(100),
				new BigDecimal(1000), 1L);

		// when
		Order order = orderMapper.toEntity(beforeTradeOrderDto, 1L);

		// then
		assertThat(order).isNotNull();
		assertThat(order.getCompanyCode()).isEqualTo(companyCode);
	}
}
