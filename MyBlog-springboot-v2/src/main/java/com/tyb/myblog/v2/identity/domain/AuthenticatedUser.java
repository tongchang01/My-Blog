package com.tyb.myblog.v2.identity.domain;

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
