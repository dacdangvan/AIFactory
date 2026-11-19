package com.vib.cms.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * LOCAL PROFILE ONLY. Supplies a deterministic development principal for the React console.
 * Never register this filter in UAT or production.
 */
@Component
@Profile("local")
public class LocalDevHeaderFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String username = request.getHeader("X-Dev-User");
        if (username == null || username.isBlank()) username = "local-dev";
        if (username.length() > 150 || !username.matches("[A-Za-z0-9@._-]+")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid X-Dev-User");
            return;
        }
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_LOCAL_DEV")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}