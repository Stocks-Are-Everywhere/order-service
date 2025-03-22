package com.onseju.orderservice.order.service.repository;

import com.onseju.orderservice.order.domain.Account;

public interface AccountRepository {

    Account getByMemberId(Long memberId);
}
