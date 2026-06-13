package com.tyb.myblog.v2.identity.domain.account;

/**
 * 允许继续刷新认证会话的后台账号快照。
 */
public record RefreshableAccount(
        long id,
        String username,
        AccountType type,
        int tokenVersion
) {
}
