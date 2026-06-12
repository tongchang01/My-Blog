package com.tyb.myblog.v2.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 单实例登录失败限流配置。
 *
 * @param loginMaxFailures 单个 IP 与用户名组合允许的连续失败次数
 * @param loginCooldown 达到阈值后的冷却时间
 * @param loginMaximumSize 登录限流缓存最大条目数
 */
@ConfigurationProperties("myblog.ratelimit")
public record LoginRateLimitProperties(
        int loginMaxFailures,
        Duration loginCooldown,
        long loginMaximumSize
) {

    public LoginRateLimitProperties {
        if (loginMaxFailures < 1) {
            throw new IllegalArgumentException("登录限流失败阈值必须大于 0");
        }
        if (loginCooldown == null || loginCooldown.isZero() || loginCooldown.isNegative()) {
            throw new IllegalArgumentException("登录限流冷却时间必须为正数");
        }
        if (loginMaximumSize < 1) {
            throw new IllegalArgumentException("登录限流缓存容量必须大于 0");
        }
    }
}
