package com.tyb.myblog.v2.stats.infrastructure.scheduling;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 访问统计调度开关。
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(
        prefix = "myblog.stats.scheduling",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class StatsSchedulingConfiguration {
}
