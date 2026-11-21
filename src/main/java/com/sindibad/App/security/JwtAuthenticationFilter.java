package com.sindibad.App.security;

import com.sindibad.App.entity.Guide;
import com.sindibad.App.entity.Person;
import com.sindibad.App.repository.GuideRepository;
import com.sindibad.App.repository.PersonRepository;
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
    private final PersonRepository personRepository;
    private final GuideRepository guideRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            PersonRepository personRepository,
            GuideRepository guideRepository) {
        this.jwtService = jwtService;
        this.personRepository = personRepository;
        this.guideRepository = guideRepository;
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

                // 6. Validate token. A GUIDE token is accepted only
                // while that guide account is still active and approved.
                if (jwtService.isTokenValid(token, email)
                        && guideTokenAllowsAuthentication(email, role)) {

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

    private boolean guideTokenAllowsAuthentication(String email, String role) {
        if (!isGuideRole(role)) {
            return true;
        }

        Person person = personRepository.findByEmail(email).orElse(null);
        if (person == null
                || person.getPersonId() == null
                || !"Active".equalsIgnoreCase(person.getAccountStatus())) {
            return false;
        }

        Guide guide = guideRepository
                .findByPersonPersonId(person.getPersonId())
                .orElse(null);

        return guide != null
                && "Approved".equalsIgnoreCase(guide.getApprovalStatus());
    }

    private boolean isGuideRole(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }

        String normalized = role.startsWith("ROLE_")
                ? role.substring("ROLE_".length())
                : role;

        return "GUIDE".equalsIgnoreCase(normalized);
    }
}