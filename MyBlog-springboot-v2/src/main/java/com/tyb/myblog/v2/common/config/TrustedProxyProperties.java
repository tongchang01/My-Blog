package com.tyb.myblog.v2.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 可信反向代理配置。
 *
 * @param trustedProxies 允许提供客户端转发头的代理 IP 或 CIDR
 */
@ConfigurationProperties("myblog.web")
public record TrustedProxyProperties(List<String> trustedProxies) {

    public TrustedProxyProperties {
        trustedProxies = trustedProxies == null
                ? List.of()
                : trustedProxies.stream()
                        .filter(value -> value != null && !value.isBlank())
                        .map(String::trim)
                        .toList();
    }
}
