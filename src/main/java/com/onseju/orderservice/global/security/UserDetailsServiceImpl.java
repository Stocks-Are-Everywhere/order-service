package com.onseju.orderservice.global.security;

import com.onseju.orderservice.global.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Member member = new Member(1L, username);
        return new UserDetailsImpl(member);
    }

    public UserDetails loadUserByIdAndUsername(Long id, String username) throws UsernameNotFoundException {
        Member member = new Member(id, username);
        return new UserDetailsImpl(member);
    }
}
