package com.tyb.myblog.v2.identity.domain.account;

/**
 * 当前登录账号的安全只读投影，仅包含资料查询所需字段。
 *
 * @param id 账号 ID
 * @param username 登录用户名
 * @param type 账号类型
 */
public record CurrentAccount(
        long id,
        String username,
        AccountType type
) {
}
