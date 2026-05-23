package com.aurora.myblog.v2.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("myblog.cors")
public record ApiCorsProperties(List<String> allowedOrigins) {
}
