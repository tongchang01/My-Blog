package com.aurora.myblog.v2.modules.identity;

import com.aurora.myblog.v2.common.config.SecurityJwtProperties;
import com.aurora.myblog.v2.common.security.auth.JwtTokenService;
import com.aurora.myblog.v2.common.security.support.InMemoryTokenRevocationStore;
import com.aurora.myblog.v2.infrastructure.security.JwtAuthTokenServiceAdapter;
import com.aurora.myblog.v2.modules.identity.application.AuthService;
import com.aurora.myblog.v2.modules.identity.application.AuthTokenService;
import com.aurora.myblog.v2.modules.identity.domain.AuthRole;
import com.aurora.myblog.v2.modules.identity.domain.LoginCommand;
import com.aurora.myblog.v2.modules.identity.infrastructure.ConfiguredUserCredentialReader;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final AuthTokenService tokenService = new JwtAuthTokenServiceAdapter(
            new JwtTokenService(
                    new SecurityJwtProperties("myblog-v2-test", "test-secret-test-secret-test-secret-123456", Duration.ofMinutes(15)),
                    new InMemoryTokenRevocationStore()));

    @Test
    void logsInWithValidCredential() {
        AuthService authService = new AuthService(
                ConfiguredUserCredentialReader.singleUser(
                        "admin@example.com",
                        passwordEncoder.encode("password123"),
                        List.of(AuthRole.ADMIN)),
                passwordEncoder,
                tokenService);

        var result = authService.login(new LoginCommand("admin@example.com", "password123"));

        assertThat(result.user().username()).isEqualTo("admin@example.com");
        assertThat(result.user().roles()).containsExactly(AuthRole.ADMIN);
        assertThat(result.token().accessToken()).isNotBlank();
    }

    @Test
    void rejectsInvalidCredentialWithSameError() {
        AuthService authService = new AuthService(
                ConfiguredUserCredentialReader.singleUser(
                        "admin@example.com",
                        passwordEncoder.encode("password123"),
                        List.of(AuthRole.ADMIN)),
                passwordEncoder,
                tokenService);

        assertThatThrownBy(() -> authService.login(new LoginCommand("admin@example.com", "wrong")))
                .hasMessage("用户名或密码错误");
    }
}
