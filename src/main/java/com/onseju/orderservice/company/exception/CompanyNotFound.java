package com.onseju.orderservice.company.exception;

import com.onseju.orderservice.global.exception.BaseException;
import org.springframework.http.HttpStatus;

public class CompanyNotFound extends BaseException {

    public CompanyNotFound() {
        super("존재하지 않는 회사 정보입니다.", HttpStatus.NOT_FOUND);
    }
}
