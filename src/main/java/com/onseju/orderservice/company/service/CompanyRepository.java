package com.onseju.orderservice.company.service;

import com.onseju.orderservice.company.domain.Company;

import java.util.List;

public interface CompanyRepository {

    List<Company> findByIsuNmContainingOrIsuAbbrvContainingOrIsuEngNmContainingOrIsuSrtCdContaining(
            final String query);

    List<Company> findAll();

    void save(final Company company);

    Company findByIsuSrtCd(final String isuSrt);
}
