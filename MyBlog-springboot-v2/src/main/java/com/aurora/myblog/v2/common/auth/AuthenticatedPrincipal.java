package com.aurora.myblog.v2.common.auth;

import java.util.List;

public record AuthenticatedPrincipal(
        String id,
        String username,
        List<String> roles
) {
    public AuthenticatedPrincipal {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
