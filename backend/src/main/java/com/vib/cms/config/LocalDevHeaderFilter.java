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
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * LOCAL PROFILE ONLY. Temporary fixed credentials for local CMS Admin UI testing.
 * Credentials: admin / admin. Never register this filter in UAT or production.
 */
@Component
@Profile("local")
public class LocalDevHeaderFilter extends OncePerRequestFilter {
    private static final String LOCAL_USERNAME = "admin";
    private static final String LOCAL_PASSWORD = "admin";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Basic ")) {
            unauthorized(response);
            return;
        }

        final String credentials;
        try {
            credentials = new String(
                    Base64.getDecoder().decode(authorization.substring(6)),
                    StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            unauthorized(response);
            return;
        }

        int separator = credentials.indexOf(':');
        if (separator < 0) {
            unauthorized(response);
            return;
        }

        String username = credentials.substring(0, separator);
        String password = credentials.substring(separator + 1);
        if (!LOCAL_USERNAME.equals(username) || !LOCAL_PASSWORD.equals(password)) {
            unauthorized(response);
            return;
        }

        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                LOCAL_USERNAME, null, List.of(new SimpleGrantedAuthority("ROLE_LOCAL_DEV")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void unauthorized(HttpServletResponse response) throws IOException {
        response.setHeader("WWW-Authenticate", "Basic realm=\"VIB CMS Local Admin\"");
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid local admin credentials");
    }
}
