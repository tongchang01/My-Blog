package com.aurora.myblog.v2.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("myblog.security.jwt")
public record SecurityJwtProperties(
        String issuer,
        String secret,
        Duration accessTokenTtl
) {
}
