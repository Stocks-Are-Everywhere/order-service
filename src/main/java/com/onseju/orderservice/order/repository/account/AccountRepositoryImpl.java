package com.onseju.orderservice.order.repository.account;

import com.onseju.orderservice.order.domain.Account;
import com.onseju.orderservice.order.exception.AccountNotFoundException;
import com.onseju.orderservice.order.service.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class AccountRepositoryImpl implements AccountRepository {

    private final AccountJpaRepository accountJpaRepository;

    @Override
    public Account getByMemberId(Long memberId) {
        return accountJpaRepository.findByMemberId(memberId)
                .orElseThrow(AccountNotFoundException::new);
    }
}
