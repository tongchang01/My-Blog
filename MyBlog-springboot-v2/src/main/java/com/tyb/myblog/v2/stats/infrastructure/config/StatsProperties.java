package com.tyb.myblog.v2.stats.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 访问统计基础设施配置。
 */
@ConfigurationProperties("myblog.stats")
public record StatsProperties(
        String hashSecret,
        int pageViewMaxRequestsPerMinute,
        long rateLimitMaximumSize,
        Scheduling scheduling) {

    public record Scheduling(
            boolean enabled,
            String aggregateCron,
            String maintenanceCron) {
    }
}
