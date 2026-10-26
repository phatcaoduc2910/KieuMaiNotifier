package com.kieumai.notifier.security;

import com.kieumai.notifier.business.service.JwtService;
import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {

            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.debug("No JWT token found in Authorization header");
                filterChain.doFilter(request, response);
                return;
            }

            String token = authHeader.substring(7);
            log.debug("JWT token extracted from header");

            if (!jwtService.isValidToken(token)) {
                log.warn("JWT token validation failed");
                filterChain.doFilter(request, response);
                return;
            }

            log.debug("JWT token is valid");


            String username = jwtService.extractUsername(token);
            if (username == null) {
                log.warn("Failed to extract username from token");
                filterChain.doFilter(request, response);
                return;
            }

            log.debug("Username extracted: {}", username);

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(username, null, null);

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.info("Authentication set for user: {}", username);

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("Error in JWT authentication filter: {}", e.getMessage());
            filterChain.doFilter(request, response);
        }
    }
}
