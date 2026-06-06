package com.tyb.myblog.v2.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.tyb.myblog.v2.common.infrastructure.persistence.audit.AuditFieldHandler;
import com.tyb.myblog.v2.common.infrastructure.persistence.audit.SecurityContextAuditor;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.Clock;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class TimeConfigTest {

    @Test
    void registersTokyoClockAndAuditBeans() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(TimeConfig.class)) {
            Clock clock = context.getBean(Clock.class);

            assertThat(clock.getZone()).isEqualTo(ZoneId.of("Asia/Tokyo"));
            assertThat(context.getBean(SecurityContextAuditor.class)).isNotNull();
            assertThat(context.getBean(MetaObjectHandler.class))
                    .isInstanceOf(AuditFieldHandler.class);
        }
    }
}
