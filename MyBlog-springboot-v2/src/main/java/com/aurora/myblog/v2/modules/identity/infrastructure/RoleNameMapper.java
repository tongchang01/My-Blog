package com.aurora.myblog.v2.modules.identity.infrastructure;

import com.aurora.myblog.v2.modules.identity.domain.AuthRole;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class RoleNameMapper {

    private RoleNameMapper() {
    }

    public static List<AuthRole> toAuthRoles(List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<AuthRole> roles = new LinkedHashSet<>();
        for (String roleName : roleNames) {
            toAuthRole(roleName).ifPresent(roles::add);
        }
        return List.copyOf(roles);
    }

    private static Optional<AuthRole> toAuthRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return Optional.empty();
        }
        return switch (roleName.trim().toLowerCase(Locale.ROOT)) {
            case "admin" -> Optional.of(AuthRole.ADMIN);
            case "user" -> Optional.of(AuthRole.USER);
            default -> Optional.empty();
        };
    }
}
