package com.tyb.myblog.v2.common.infrastructure.persistence.audit;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityContextAuditorTest {

    private final SecurityContextAuditor auditor = new SecurityContextAuditor();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsAuthenticatedPrincipalIdAsLong() {
        authenticate(new AuthenticatedPrincipal("123456789", "admin", List.of("ADMIN")));

        assertThat(auditor.currentUserId()).isEqualTo(123456789L);
    }

    @Test
    void returnsNullWithoutAuthenticatedPrincipal() {
        assertThat(auditor.currentUserId()).isNull();

        authenticate("anonymousUser");
        assertThat(auditor.currentUserId()).isNull();
    }

    @Test
    void rejectsNonNumericAuthenticatedPrincipalId() {
        authenticate(new AuthenticatedPrincipal("invalid-id", "admin", List.of("ADMIN")));

        assertThatThrownBy(auditor::currentUserId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("当前登录用户 ID 不是有效的 Long");
    }

    private void authenticate(Object principal) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
    }
}
