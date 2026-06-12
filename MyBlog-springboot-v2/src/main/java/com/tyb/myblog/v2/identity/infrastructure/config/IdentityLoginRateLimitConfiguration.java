package com.tyb.myblog.v2.identity.infrastructure.config;

import com.github.benmanes.caffeine.cache.Ticker;
import com.tyb.myblog.v2.common.config.LoginRateLimitProperties;
import com.tyb.myblog.v2.identity.domain.auth.LoginRateLimiter;
import com.tyb.myblog.v2.identity.infrastructure.ratelimit.CaffeineLoginRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 后台登录限流基础设施配置。
 */
@Configuration
public class IdentityLoginRateLimitConfiguration {

    /**
     * 创建生产环境使用的单调时钟。
     */
    @Bean
    Ticker loginRateLimitTicker() {
        return Ticker.systemTicker();
    }

    /**
     * 创建单实例 Caffeine 登录失败限流器。
     */
    @Bean
    LoginRateLimiter loginRateLimiter(
            LoginRateLimitProperties properties,
            Ticker loginRateLimitTicker
    ) {
        return new CaffeineLoginRateLimiter(properties, loginRateLimitTicker);
    }
}
