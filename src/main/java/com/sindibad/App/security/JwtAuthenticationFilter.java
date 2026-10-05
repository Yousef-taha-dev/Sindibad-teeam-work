package com.sindibad.App.security;

import com.sindibad.App.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Extract Authorization header
        final String authHeader = request.getHeader("Authorization");

        // 2. Check if header is missing or not a Bearer token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extract token
        final String token = authHeader.substring(7);

        try {
            final String email = jwtService.extractUsername(token);
            final String role = jwtService.extractRole(token);

            // 4. Authenticate if context is empty
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                if (jwtService.isTokenValid(token, email)) {

                    // Build authorities list safely
                    List<SimpleGrantedAuthority> authorities = Collections.emptyList();
                    if (role != null && !role.isBlank()) {
                        String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                        authorities = List.of(new SimpleGrantedAuthority(authority));
                    }

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    email,
                                    null,
                                    authorities
                            );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Clear context if token parsing or validation fails
            SecurityContextHolder.clearContext();
        }

        // 5. Continue filter chain
        filterChain.doFilter(request, response);
    }
}