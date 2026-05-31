package com.tyb.myblog.v2.identity.domain;

import java.util.Set;

/**
 * 已通过认证的用户。
 *
 * <p>用于应用层表达登录成功后的身份，不直接等同于数据库用户表或用户资料表。</p>
 *
 * @param id       认证账号 ID，对应旧库 {@code t_user_auth.id}
 * @param username 登录用户名，对应旧库 {@code t_user_auth.username}
 * @param roles    用户角色集合
 */
public record AuthenticatedUser(
        String id,
        String username,
        Set<AuthRole> roles
) {
    /**
     * 判断当前用户是否拥有指定角色。
     */
    public boolean hasRole(AuthRole role) {
        return roles.contains(role);
    }
}
