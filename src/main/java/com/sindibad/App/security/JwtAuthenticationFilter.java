package com.sindibad.App.security;

import com.sindibad.App.service.JwtService;
import io.jsonwebtoken.JwtException;
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

        // 1. Get Authorization header
        final String authHeader = request.getHeader("Authorization");

        // 2. Check Bearer token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extract JWT
        final String token = authHeader.substring(7);

        try {
            // 4. Extract email and role from JWT
            final String email = jwtService.extractUsername(token);
            final String role = jwtService.extractRole(token);

            // 5. Authenticate only if there is no existing authentication
            if (email != null &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {

                // 6. Validate token
                if (jwtService.isTokenValid(token, email)) {

                    // 7. Convert role to Spring Security authority
                    List<SimpleGrantedAuthority> authorities =
                            Collections.emptyList();

                    if (role != null && !role.isBlank()) {

                        String authority = role.startsWith("ROLE_")
                                ? role
                                : "ROLE_" + role;

                        authorities = List.of(
                                new SimpleGrantedAuthority(authority)
                        );
                    }

                    // 8. Create authenticated user
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    email,
                                    null,
                                    authorities
                            );

                    // 9. Add request details
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    // 10. Store authentication
                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(authToken);
                }
            }

        } catch (JwtException | IllegalArgumentException e) {

            // Invalid or expired JWT
            SecurityContextHolder.clearContext();
        }

        // 11. Continue request
        filterChain.doFilter(request, response);
    }
}