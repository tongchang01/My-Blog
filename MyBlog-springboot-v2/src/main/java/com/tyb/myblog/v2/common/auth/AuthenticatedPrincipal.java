package com.tyb.myblog.v2.common.auth;

import java.util.List;

/**
 * 当前已认证用户的接口层身份对象。
 *
 * <p>该对象会写入 Spring Security 上下文，并通过 {@link CurrentUser} 注入到 Controller 方法。
 * 这里只保留接口鉴权和审计需要的最小信息，不承载完整用户资料。</p>
 *
 * @param id       用户 ID
 * @param username 登录用户名
 * @param roles    用户角色名称列表，不包含 {@code ROLE_} 前缀
 */
public record AuthenticatedPrincipal(
        String id,
        String username,
        List<String> roles
) {
    /**
     * 复制角色列表，避免外部集合在认证后继续被修改。
     */
    public AuthenticatedPrincipal {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
