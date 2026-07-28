package com.tyb.myblog.v2.common.auth;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticatedPrincipalTest {

    @Test
    void centralizesAdminAndReadOnlyRoleChecks() {
        AuthenticatedPrincipal admin = principal(List.of(AuthenticatedPrincipal.ADMIN_ROLE));
        AuthenticatedPrincipal demo = principal(List.of(AuthenticatedPrincipal.DEMO_ROLE));
        AuthenticatedPrincipal other = principal(List.of("OTHER"));
        AuthenticatedPrincipal withoutRoles = principal(null);

        assertThat(admin.isAdmin()).isTrue();
        assertThat(admin.canReadAdminResources()).isTrue();
        assertThat(demo.isAdmin()).isFalse();
        assertThat(demo.canReadAdminResources()).isTrue();
        assertThat(other.canReadAdminResources()).isFalse();
        assertThat(withoutRoles.canReadAdminResources()).isFalse();
    }

    private AuthenticatedPrincipal principal(List<String> roles) {
        return new AuthenticatedPrincipal("1001", "user", roles);
    }
}
