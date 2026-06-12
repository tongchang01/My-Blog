package com.tyb.myblog.v2.identity.domain.auth;

import com.tyb.myblog.v2.identity.domain.account.UserAccount;

/**
 * 后台登录凭据校验结果。
 */
public sealed interface LoginCredentialResult
        permits LoginCredentialResult.Authenticated,
                LoginCredentialResult.BadCredentials,
                LoginCredentialResult.Locked {

    /**
     * 账号和密码有效，携带同一次查询得到的账号进入后续登录编排。
     *
     * @param account 已完成凭据校验的账号
     */
    record Authenticated(UserAccount account) implements LoginCredentialResult {
    }

    /**
     * 用户名、账号类型或密码不符合登录要求。
     */
    enum BadCredentials implements LoginCredentialResult {
        INSTANCE
    }

    /**
     * 账号仍处于持久化锁定期。
     */
    enum Locked implements LoginCredentialResult {
        INSTANCE
    }
}
