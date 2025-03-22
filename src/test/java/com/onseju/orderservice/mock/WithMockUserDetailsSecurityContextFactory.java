package com.onseju.orderservice.mock;

import com.onseju.orderservice.global.entity.Member;
import com.onseju.orderservice.global.security.UserDetailsImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithMockUserDetailsSecurityContextFactory implements WithSecurityContextFactory<WithMockUserDetails> {

    @Override
    public SecurityContext createSecurityContext(WithMockUserDetails annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        UserDetailsImpl userDetails = new UserDetailsImpl(
                Member.builder()
                        .username(annotation.username())
                        .id(Long.valueOf(annotation.userId()))
                        .build()
        );

        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken
                = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        context.setAuthentication(usernamePasswordAuthenticationToken);
        return context;
    }
}
