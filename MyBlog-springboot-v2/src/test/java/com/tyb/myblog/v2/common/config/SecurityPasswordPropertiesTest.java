package com.tyb.myblog.v2.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 后台账号密码安全配置绑定测试。
 */
@ActiveProfiles("test")
@SpringBootTest
class SecurityPasswordPropertiesTest {

    @Autowired
    private SecurityPasswordProperties properties;

    @Test
    void bindsPasswordSecuritySettings() {
        assertThat(properties.loginMaxAttempts()).isEqualTo(5);
        assertThat(properties.loginCooldown()).isEqualTo(Duration.ofMinutes(10));
        assertThat(properties.bcryptStrength()).isEqualTo(10);
    }
}
