package com.tyb.myblog.v2.identity.domain.account;

import java.time.LocalDateTime;

/**
 * 登录账号。
 *
 * @param id 账号主键
 * @param username 登录用户名
 * @param passwordHash 密码哈希
 * @param type 账号类型
 * @param tokenVersion 令牌版本
 * @param loginFailCount 连续登录失败次数
 * @param lockedUntil 锁定截止时间
 */
public record UserAccount(
        long id,
        String username,
        String passwordHash,
        AccountType type,
        int tokenVersion,
        int loginFailCount,
        LocalDateTime lockedUntil
) {

    /**
     * 判断当前账号是否允许登录后台。
     *
     * @return 允许登录时返回 {@code true}
     */
    public boolean canLoginToAdmin() {
        return type.canLoginToAdmin();
    }

    /**
     * 判断账号在指定时间是否处于锁定状态。
     *
     * @param now 判断时间
     * @return 锁定截止时间晚于判断时间时返回 {@code true}
     */
    public boolean isLockedAt(LocalDateTime now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }
}
