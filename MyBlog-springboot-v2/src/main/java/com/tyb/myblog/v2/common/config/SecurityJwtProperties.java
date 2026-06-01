package com.tyb.myblog.v2.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT 配置项。
 *
 * <p>绑定 {@code myblog.security.jwt}。生产环境必须通过环境变量或安全配置中心提供密钥，
 * 不能使用开发环境的测试值。</p>
 *
 * @param issuer         JWT 签发者标识，用于区分当前系统签发的 token
 * @param secret         JWT HMAC 签名密钥，生产环境应使用足够长度且不可提交到 Git 的密钥
 * @param accessTokenTtl 访问令牌有效期，过长会扩大 token 泄露后的风险窗口
 */
@ConfigurationProperties("myblog.security.jwt")
public record SecurityJwtProperties(
        String issuer,
        String secret,
        Duration accessTokenTtl
) {
}
