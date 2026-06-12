package com.tyb.myblog.v2.identity.infrastructure.security;

import com.tyb.myblog.v2.identity.domain.auth.PasswordHashVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Security 密码摘要校验适配器测试。
 */
class SpringPasswordHashVerifierTest {

    @Test
    void delegatesToSpringPasswordEncoder() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
        PasswordHashVerifier verifier = new SpringPasswordHashVerifier(encoder);
        String hash = encoder.encode("correct-password");

        assertThat(verifier.matches("correct-password", hash)).isTrue();
        assertThat(verifier.matches("wrong-password", hash)).isFalse();
    }
}
