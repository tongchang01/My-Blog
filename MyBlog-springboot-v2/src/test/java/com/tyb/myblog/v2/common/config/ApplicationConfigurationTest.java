package com.tyb.myblog.v2.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyb.myblog.v2.identity.domain.auth.LoginRateLimiter;
import com.tyb.myblog.v2.identity.infrastructure.ratelimit.CaffeineLoginRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.ClassUtils;

import java.time.LocalDateTime;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class ApplicationConfigurationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Environment environment;

    @Autowired
    private LoginRateLimiter loginRateLimiter;

    @Test
    void configuresJacksonForAsiaTokyoIsoDateTime() throws Exception {
        assertThat(objectMapper.getSerializationConfig().getTimeZone())
                .isEqualTo(TimeZone.getTimeZone("Asia/Tokyo"));
        assertThat(objectMapper.writeValueAsString(LocalDateTime.of(2026, 6, 3, 14, 30)))
                .isEqualTo("\"2026-06-03T14:30:00\"");
    }

    @Test
    void enablesOpenApiAndKnife4jForTestProfile() {
        assertThat(environment.getProperty("springdoc.api-docs.enabled", Boolean.class))
                .isTrue();
        assertThat(environment.getProperty("springdoc.swagger-ui.enabled", Boolean.class))
                .isTrue();
        assertThat(environment.getProperty("knife4j.enable", Boolean.class))
                .isTrue();
    }

    @Test
    void providesKnife4jSpringBoot3Starter() {
        assertThat(ClassUtils.isPresent(
                "com.github.xiaoymin.knife4j.spring.configuration.Knife4jAutoConfiguration",
                getClass().getClassLoader()))
                .isTrue();
    }

    @Test
    void shouldProvideLoginRateLimiter() {
        assertThat(loginRateLimiter).isInstanceOf(CaffeineLoginRateLimiter.class);
    }
}
