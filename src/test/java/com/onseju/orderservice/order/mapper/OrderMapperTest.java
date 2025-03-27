package com.onseju.orderservice.order.mapper;


import com.onseju.orderservice.order.domain.Order;
import com.onseju.orderservice.order.domain.Type;
import com.onseju.orderservice.order.dto.BeforeTradeOrderDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Nested
@DisplayName("Entity & Dto 변환 테스트")
class CompanyMapperTest {

	CompanyMapper mapper = new CompanyMapper();

	@Test
	void toCompanySearchResponse() {
		// given

		String companyCode = "005930";
		BeforeTradeOrderDto beforeTradeOrderDto = new BeforeTradeOrderDto("005930", Type.LIMIT_BUY, new BigDecimal(100),
				new BigDecimal(1000), 1L);

		// when
		Order order = orderMapper.toEntity(beforeTradeOrderDto, 1L);

		// then
		assertThat(response).isNotNull();
		assertThat(response.isuNm()).isEqualTo("삼성전자");
	}
}
