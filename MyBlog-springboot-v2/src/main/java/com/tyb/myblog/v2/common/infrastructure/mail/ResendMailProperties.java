package com.tyb.myblog.v2.common.infrastructure.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "myblog.mail.resend")
public record ResendMailProperties(
        boolean enabled,
        String apiKey,
        String fromEmail) {
}
