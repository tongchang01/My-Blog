package com.tyb.myblog.v2.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 后台账号密码与登录锁定配置。
 *
 * @param loginMaxAttempts 单个账号连续密码错误锁定阈值
 * @param loginCooldown 持久化锁定时长
 * @param bcryptStrength BCrypt 计算强度
 */
@ConfigurationProperties("myblog.security.password")
public record SecurityPasswordProperties(
        int loginMaxAttempts,
        Duration loginCooldown,
        int bcryptStrength
) {

    public SecurityPasswordProperties {
        if (loginMaxAttempts < 1) {
            throw new IllegalArgumentException("登录失败阈值必须大于 0");
        }
        if (loginCooldown == null || loginCooldown.isZero() || loginCooldown.isNegative()) {
            throw new IllegalArgumentException("登录锁定时长必须为正数");
        }
        if (bcryptStrength < 4 || bcryptStrength > 31) {
            throw new IllegalArgumentException("BCrypt 强度必须在 4 到 31 之间");
        }
    }
}
