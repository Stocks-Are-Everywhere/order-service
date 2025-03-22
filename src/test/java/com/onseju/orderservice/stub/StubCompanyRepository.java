package com.onseju.orderservice.stub;

import com.onseju.orderservice.company.domain.Company;
import com.onseju.orderservice.company.service.CompanyRepository;

import java.math.BigDecimal;
import java.util.List;

public class StubCompanyRepository implements CompanyRepository {

    private final Company company = Company.builder()
            .isuCd("005930")
            .isuSrtCd("005930")
            .isuNm("삼성전자")
            .isuAbbrv("KOSPI")
            .isuEngNm("주권")
            .kindStkcertTpNm("삼성전자")
            .closingPrice(new BigDecimal(1000))
            .build();

    @Override
    public List<Company> findByIsuNmContainingOrIsuAbbrvContainingOrIsuEngNmContainingOrIsuSrtCdContaining(String query) {
        return List.of(company);
    }

    @Override
    public List<Company> findAll() {
        return List.of(company);
    }

    @Override
    public void save(Company company) {

    }

    @Override
    public Company findByIsuSrtCd(String isuSrt) {
        return company;
    }
}
