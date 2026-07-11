package com.tyb.myblog.v2.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 首个管理员一次性初始化配置。
 */
@ConfigurationProperties("myblog.bootstrap-admin")
public record BootstrapAdminProperties(
        boolean enabled,
        String username,
        String password,
        boolean exitAfterRun
) {
}
