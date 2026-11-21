package com.sindibad.App.security;

import com.sindibad.App.entity.Guide;
import com.sindibad.App.entity.Person;
import com.sindibad.App.repository.GuideRepository;
import com.sindibad.App.repository.PersonRepository;
import com.sindibad.App.service.JwtService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuideTokenAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private PersonRepository personRepository;
    @Mock
    private GuideRepository guideRepository;
    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(
                jwtService,
                personRepository,
                guideRepository
        );
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void approvedActiveGuideIsAuthenticated() throws Exception {
        Person person = person("Active");
        when(jwtService.extractUsername("guide-token"))
                .thenReturn("lina@example.com");
        when(jwtService.extractRole("guide-token")).thenReturn("GUIDE");
        when(jwtService.isTokenValid("guide-token", "lina@example.com"))
                .thenReturn(true);
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person));
        when(guideRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(guide("Approved")));

        filter.doFilter(request("guide-token"), new MockHttpServletResponse(), filterChain);

        assertEquals(
                "lina@example.com",
                SecurityContextHolder.getContext().getAuthentication().getName()
        );
        assertTrue(hasAuthority("ROLE_GUIDE"));
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void rejectedGuideIsNotAuthenticated() throws Exception {
        denyGuide("Rejected");

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void suspendedGuideIsNotAuthenticated() throws Exception {
        denyGuide("Suspended");

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void inactivePersonIsNotAuthenticated() throws Exception {
        stubGuideToken();
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person("Inactive")));

        filter.doFilter(request("guide-token"), new MockHttpServletResponse(), filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(guideRepository, never()).findByPersonPersonId(any());
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void missingPersonIsNotAuthenticated() throws Exception {
        stubGuideToken();
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.empty());

        filter.doFilter(request("guide-token"), new MockHttpServletResponse(), filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(guideRepository, never()).findByPersonPersonId(any());
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void missingGuideIsNotAuthenticated() throws Exception {
        stubGuideToken();
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person("Active")));
        when(guideRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.empty());

        filter.doFilter(request("guide-token"), new MockHttpServletResponse(), filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void repositoryFailureDoesNotAuthenticate() {
        stubGuideToken();
        when(personRepository.findByEmail("lina@example.com"))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertThrows(
                IllegalStateException.class,
                () -> filter.doFilter(
                        request("guide-token"),
                        new MockHttpServletResponse(),
                        filterChain
                )
        );

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void adminTokenSkipsGuideLookup() throws Exception {
        stubRoleToken("admin-token", "admin@example.com", "ADMIN");

        filter.doFilter(request("admin-token"), new MockHttpServletResponse(), filterChain);

        assertEquals(
                "admin@example.com",
                SecurityContextHolder.getContext().getAuthentication().getName()
        );
        assertTrue(hasAuthority("ROLE_ADMIN"));
        verify(personRepository, never()).findByEmail(any());
        verify(guideRepository, never()).findByPersonPersonId(any());
    }

    @Test
    void touristTokenSkipsGuideLookup() throws Exception {
        stubRoleToken("tourist-token", "lina@example.com", "TOURIST");

        filter.doFilter(request("tourist-token"), new MockHttpServletResponse(), filterChain);

        assertEquals(
                "lina@example.com",
                SecurityContextHolder.getContext().getAuthentication().getName()
        );
        assertTrue(hasAuthority("ROLE_TOURIST"));
        verify(personRepository, never()).findByEmail(any());
        verify(guideRepository, never()).findByPersonPersonId(any());
    }

    private void denyGuide(String approvalStatus) throws Exception {
        stubGuideToken();
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person("Active")));
        when(guideRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(guide(approvalStatus)));

        filter.doFilter(request("guide-token"), new MockHttpServletResponse(), filterChain);
    }

    private void stubGuideToken() {
        stubRoleToken("guide-token", "lina@example.com", "GUIDE");
    }

    private void stubRoleToken(String token, String email, String role) {
        when(jwtService.extractUsername(token)).thenReturn(email);
        when(jwtService.extractRole(token)).thenReturn(role);
        when(jwtService.isTokenValid(token, email)).thenReturn(true);
    }

    private MockHttpServletRequest request(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    private boolean hasAuthority(String authority) {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }

    private Person person(String accountStatus) {
        Person person = new Person();
        person.setPersonId(3L);
        person.setEmail("lina@example.com");
        person.setAccountStatus(accountStatus);
        return person;
    }

    private Guide guide(String approvalStatus) {
        Guide guide = new Guide();
        guide.setGuideId(9L);
        guide.setApprovalStatus(approvalStatus);
        return guide;
    }
}
