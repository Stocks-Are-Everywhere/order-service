package com.onseju.orderservice.stub;

import com.onseju.orderservice.order.domain.Account;
import com.onseju.orderservice.order.service.repository.AccountRepository;

import java.math.BigDecimal;

public class StubAccountRepository implements AccountRepository {

    private final Account account = new Account(1L , new BigDecimal("100000000"), new BigDecimal(0), 1L);

    @Override
    public Account getByMemberId(Long memberId) {
        return account;
    }
}
