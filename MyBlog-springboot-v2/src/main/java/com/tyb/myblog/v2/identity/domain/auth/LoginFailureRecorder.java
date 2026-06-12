package com.tyb.myblog.v2.identity.domain.auth;

import java.time.LocalDateTime;

/**
 * 后台登录密码失败状态记录端口。
 */
public interface LoginFailureRecorder {

    /**
     * 原子记录一次已确认后台账号的密码错误。
     *
     * @param userId 账号 ID
     * @param failedAt 密码校验失败时间
     * @param maxAttempts 单个失败周期的锁定阈值
     * @param lockedUntil 达到阈值时写入的锁定截止时间
     */
    void recordPasswordFailure(
            long userId,
            LocalDateTime failedAt,
            int maxAttempts,
            LocalDateTime lockedUntil);
}
