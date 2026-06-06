package com.tyb.myblog.v2.common.infrastructure.persistence.audit;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 从 Spring Security 上下文解析数据库审计用户 ID。
 */
public class SecurityContextAuditor {

    /**
     * 获取当前系统用户 ID。
     *
     * @return 已认证系统用户的 Long 型 ID；匿名请求或系统任务返回 null
     */
    public Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedPrincipal principal)) {
            return null;
        }

        try {
            return Long.valueOf(principal.id());
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("当前登录用户 ID 不是有效的 Long", exception);
        }
    }
}
