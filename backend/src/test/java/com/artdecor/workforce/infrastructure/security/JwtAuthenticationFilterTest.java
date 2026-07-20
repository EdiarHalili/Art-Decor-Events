package com.artdecor.workforce.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.artdecor.workforce.domain.UserRole;
import com.artdecor.workforce.domain.UserStatus;
import com.artdecor.workforce.infrastructure.persistence.EmployeeRepository;
import com.artdecor.workforce.infrastructure.persistence.UserAccountEntity;
import com.artdecor.workforce.infrastructure.persistence.UserAccountRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class JwtAuthenticationFilterTest {
    private final JwtTokenService tokens = new JwtTokenService(new JwtProperties(
            "test-secret-key-that-is-long-enough-for-hmac-signing",
            "art-decor-test",
            60
    ));
    private final UserAccountRepository users = org.mockito.Mockito.mock(UserAccountRepository.class);
    private final EmployeeRepository employees = org.mockito.Mockito.mock(EmployeeRepository.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokens, users, employees);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesActiveUserWithCurrentTokenVersion() throws Exception {
        UUID userId = UUID.randomUUID();
        UserAccountEntity user = user(userId, UserRole.ADMINISTRATOR, UserStatus.ACTIVE, 2);
        when(users.findById(userId)).thenReturn(Optional.of(user));

        filter.doFilter(request(tokens.issueToken(userId, UserRole.ADMINISTRATOR, null, 2)),
                new MockHttpServletResponse(),
                new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void rejectsOldTokenVersionAfterAccountChange() throws Exception {
        UUID userId = UUID.randomUUID();
        UserAccountEntity user = user(userId, UserRole.ADMINISTRATOR, UserStatus.ACTIVE, 3);
        when(users.findById(userId)).thenReturn(Optional.of(user));

        filter.doFilter(request(tokens.issueToken(userId, UserRole.ADMINISTRATOR, null, 2)),
                new MockHttpServletResponse(),
                new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void rejectsInactiveUsers() throws Exception {
        UUID userId = UUID.randomUUID();
        UserAccountEntity user = user(userId, UserRole.ADMINISTRATOR, UserStatus.INACTIVE, 0);
        when(users.findById(userId)).thenReturn(Optional.of(user));

        filter.doFilter(request(tokens.issueToken(userId, UserRole.ADMINISTRATOR, null, 0)),
                new MockHttpServletResponse(),
                new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private MockHttpServletRequest request(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    private UserAccountEntity user(UUID userId, UserRole role, UserStatus status, int tokenVersion) {
        UserAccountEntity user = new UserAccountEntity();
        ReflectionTestUtils.setField(user, "id", userId);
        user.setFullName("User");
        user.setEmail("user@example.com");
        user.setPasswordHash("hash");
        user.setRole(role);
        user.setStatus(status);
        user.setTokenVersion(tokenVersion);
        return user;
    }
}
