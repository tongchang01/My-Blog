package com.aurora.myblog.v2.modules.identity.domain;

import java.util.Set;

public record AuthenticatedUser(
        String id,
        String username,
        Set<AuthRole> roles
) {
    public boolean hasRole(AuthRole role) {
        return roles.contains(role);
    }
}
