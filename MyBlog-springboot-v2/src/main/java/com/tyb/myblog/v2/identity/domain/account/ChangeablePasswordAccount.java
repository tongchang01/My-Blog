package com.tyb.myblog.v2.identity.domain.account;

/**
 * 修改密码事务所需的最小账号快照。
 *
 * @param id 账号 ID
 * @param type 账号类型
 * @param passwordHash 当前 BCrypt 密码摘要
 */
public record ChangeablePasswordAccount(
        long id,
        AccountType type,
        String passwordHash
) {
}
