package com.tyb.myblog.v2.common.security.jwt;

import com.tyb.myblog.v2.common.config.SecurityJwtProperties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

/**
 * JWT 密钥启动校验器。
 *
 * <p>启动阶段拒绝空密钥和开发默认密钥，避免生产环境在缺少真实
 * {@code MYBLOG_JWT_SECRET} 时仍然使用固定字符串签发令牌。</p>
 */
@Component
public class JwtSecretStartupValidator implements InitializingBean {

    /**
     * 禁止用于任何运行环境的开发默认密钥。
     */
    private static final String DEFAULT_DEVELOPMENT_SECRET = "change-me-change-me-change-me-change-me";

    /**
     * JWT 配置项。
     */
    private final SecurityJwtProperties properties;

    public JwtSecretStartupValidator(SecurityJwtProperties properties) {
        this.properties = properties;
    }

    /**
     * 校验 JWT 签名密钥是否可用于当前运行环境。
     */
    @Override
    public void afterPropertiesSet() {
        String secret = properties.secret();
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("JWT 密钥不能为空，请配置 MYBLOG_JWT_SECRET 或环境专用测试密钥。");
        }
        if (DEFAULT_DEVELOPMENT_SECRET.equals(secret)) {
            throw new IllegalStateException("JWT 密钥不能使用默认开发值，请配置真实密钥。");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT 密钥按 UTF-8 编码后不能少于 32 字节。");
        }
    }
}
