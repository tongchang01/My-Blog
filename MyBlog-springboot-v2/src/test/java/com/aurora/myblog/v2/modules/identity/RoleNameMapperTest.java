package com.aurora.myblog.v2.modules.identity;

import com.aurora.myblog.v2.modules.identity.domain.AuthRole;
import com.aurora.myblog.v2.modules.identity.infrastructure.RoleNameMapper;
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
