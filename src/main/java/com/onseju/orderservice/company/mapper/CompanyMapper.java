package com.onseju.orderservice.company.mapper;

import com.onseju.orderservice.company.controller.response.CompanySearchResponse;
import com.onseju.orderservice.company.domain.Company;
import org.springframework.stereotype.Component;

@Component
public class CompanyMapper {

    public CompanySearchResponse toCompanySearchResponse(Company company) {
        return new CompanySearchResponse(
                company.getIsuNm(),
                company.getIsuSrtCd(),
                company.getIsuAbbrv(),
                company.getIsuEngNm(),
                company.getKindStkcertTpNm());
    }
}
