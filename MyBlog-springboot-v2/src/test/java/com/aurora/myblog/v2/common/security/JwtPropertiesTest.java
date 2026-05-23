package com.aurora.myblog.v2.common.security;

import com.aurora.myblog.v2.common.config.SecurityJwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class JwtPropertiesTest {

    @Autowired
    private SecurityJwtProperties jwtProperties;

    @Test
    void bindsJwtSettings() {
        assertThat(jwtProperties.issuer()).isEqualTo("myblog-v2-test");
        assertThat(jwtProperties.accessTokenTtl()).isEqualTo(Duration.ofSeconds(900));
        assertThat(jwtProperties.secret()).hasSizeGreaterThanOrEqualTo(32);
    }
}
