package com.onseju.orderservice.company.mapper;

import com.onseju.orderservice.company.controller.response.CompanySearchResponse;
import com.onseju.orderservice.company.domain.Company;
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
        Company company = Company.builder()
                .isuCd("005930")
                .isuSrtCd("005930")
                .isuNm("삼성전자")
                .isuAbbrv("KOSPI")
                .isuEngNm("주권")
                .kindStkcertTpNm("삼성전자")
                .closingPrice(new BigDecimal(1000))
                .build();
        // when
        CompanySearchResponse response = mapper.toCompanySearchResponse(company);

        // then
        assertThat(response).isNotNull();
        assertThat(response.isuNm()).isEqualTo("삼성전자");
    }
}