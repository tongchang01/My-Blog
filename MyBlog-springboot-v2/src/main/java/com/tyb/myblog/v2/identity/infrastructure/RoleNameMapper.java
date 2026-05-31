package com.tyb.myblog.v2.identity.infrastructure;

import com.tyb.myblog.v2.identity.domain.AuthRole;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 旧库角色名称转换器。
 *
 * <p>旧库 {@code t_role.role_name} 保存字符串角色名，V2 在领域层统一使用 {@link AuthRole}。</p>
 */
public final class RoleNameMapper {

    private RoleNameMapper() {
    }

    /**
     * 将旧库角色名称列表转换为领域角色列表。
     */
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

    /**
     * 转换单个旧库角色名称。
     */
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
