package com.onseju.orderservice.order.repository.account;

import com.onseju.orderservice.order.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountJpaRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByMemberId(Long memberId);
}
