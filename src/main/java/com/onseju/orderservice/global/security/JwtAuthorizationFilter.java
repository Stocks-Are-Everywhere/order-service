package com.onseju.orderservice.global.security;

import com.onseju.orderservice.global.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;

@Slf4j(topic = "JWT Authorization Filter")
@RequiredArgsConstructor
public class JwtAuthorizationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain) throws ServletException, IOException {
        String accessToken = jwtUtil.getJwtFromHeader(req);
        if (StringUtils.hasText(accessToken)) {
            if (!jwtUtil.validateToken(accessToken)) {
                res.setStatus(401); // Unauthorized
                writeResponse(res, "Invalid or expired token.");
                return;
            }

            Claims info = jwtUtil.getUserInfoFromToken(accessToken);
            log.info("Token Authorization Success: {}", info.getSubject());

            try {
                setAuthentication(Long.parseLong(info.get("userId").toString()), info.getSubject());
            } catch (Exception e) {
                log.error("Authentication Error: {}", e.getMessage());
                return;
            }
        }

        filterChain.doFilter(req, res);
    }

    private void setAuthentication(Long id, String username) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication authentication = createAuthentication(id, username);
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    private Authentication createAuthentication(Long id, String username) {
        UserDetails userDetails = userDetailsService.loadUserByIdAndUsername(id, username);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    private void writeResponse(HttpServletResponse response, String message) {
        try {
            response.setContentType("text/plain");
            PrintWriter writer = response.getWriter();
            writer.write(message);
            writer.flush();
        } catch (IOException e) {
            log.error("Error writing response: {}", e.getMessage());
        }
    }
}
