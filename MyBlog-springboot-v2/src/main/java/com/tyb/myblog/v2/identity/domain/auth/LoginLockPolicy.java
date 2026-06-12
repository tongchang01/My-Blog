package com.tyb.myblog.v2.identity.domain.auth;

import java.time.Duration;

/**
 * 后台账号持久化锁定规则。
 *
 * @param maxAttempts 单个失败周期允许的最大密码错误次数
 * @param lockDuration 达到阈值后的锁定时长
 */
public record LoginLockPolicy(int maxAttempts, Duration lockDuration) {

    public LoginLockPolicy {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("登录失败阈值必须大于 0");
        }
        if (lockDuration == null || lockDuration.isZero() || lockDuration.isNegative()) {
            throw new IllegalArgumentException("登录锁定时长必须为正数");
        }
    }
}
