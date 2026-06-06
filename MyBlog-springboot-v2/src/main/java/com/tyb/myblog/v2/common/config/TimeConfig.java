package com.tyb.myblog.v2.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.tyb.myblog.v2.common.infrastructure.persistence.audit.AuditFieldHandler;
import com.tyb.myblog.v2.common.infrastructure.persistence.audit.SecurityContextAuditor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * 全站时间与持久化审计配置。
 */
@Configuration
public class TimeConfig {

    private static final ZoneId SYSTEM_ZONE = ZoneId.of("Asia/Tokyo");

    /**
     * 提供固定为 Asia/Tokyo 的系统时钟。
     */
    @Bean
    Clock clock() {
        return Clock.system(SYSTEM_ZONE);
    }

    /**
     * 提供当前系统用户审计解析器。
     */
    @Bean
    SecurityContextAuditor securityContextAuditor() {
        return new SecurityContextAuditor();
    }

    /**
     * 注册 MyBatis-Plus 审计字段自动填充器。
     */
    @Bean
    MetaObjectHandler auditFieldHandler(Clock clock, SecurityContextAuditor auditor) {
        return new AuditFieldHandler(clock, auditor);
    }
}
