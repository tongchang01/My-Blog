package com.tyb.myblog.v2.identity.infrastructure.security;

import com.tyb.myblog.v2.identity.domain.auth.PasswordHashService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Security 密码摘要服务测试。
 */
class SpringPasswordHashServiceTest {

    @Test
    void encodesAndMatchesPasswordWithSpringEncoder() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
        PasswordHashService service = new SpringPasswordHashService(encoder);

        String passwordHash = service.encode("new-password");

        assertThat(passwordHash).isNotEqualTo("new-password");
        assertThat(service.matches("new-password", passwordHash)).isTrue();
        assertThat(service.matches("wrong-password", passwordHash)).isFalse();
    }
}
