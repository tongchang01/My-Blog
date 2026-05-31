package com.tyb.myblog.v2.identity;

import com.tyb.myblog.v2.identity.domain.AuthRole;
import com.tyb.myblog.v2.identity.infrastructure.RoleNameMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoleNameMapperTest {

    @Test
    void mapsLegacyRoleNamesToV2Roles() {
        List<AuthRole> roles = RoleNameMapper.toAuthRoles(List.of("admin", "user", "test", "ADMIN"));

        assertThat(roles).containsExactly(AuthRole.ADMIN, AuthRole.USER);
    }

    @Test
    void returnsEmptyListForUnknownOrBlankRoles() {
        List<AuthRole> roles = RoleNameMapper.toAuthRoles(List.of("", " ", "test", "operator"));

        assertThat(roles).isEmpty();
    }
}
