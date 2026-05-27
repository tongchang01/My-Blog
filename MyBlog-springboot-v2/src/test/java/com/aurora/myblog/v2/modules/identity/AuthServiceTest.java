package com.aurora.myblog.v2.modules.identity;

import com.aurora.myblog.v2.common.error.ApiException;
import com.aurora.myblog.v2.modules.identity.application.AuthService;
import com.aurora.myblog.v2.modules.identity.application.AuthTokenService;
import com.aurora.myblog.v2.modules.identity.domain.AuthRole;
import com.aurora.myblog.v2.modules.identity.domain.LoginAuditRecorder;
import com.aurora.myblog.v2.modules.identity.domain.LoginCommand;
import com.aurora.myblog.v2.modules.identity.infrastructure.ConfiguredUserCredentialReader;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void logsInAndRecordsSuccessfulAudit() {
        RecordingLoginAuditRecorder auditRecorder = new RecordingLoginAuditRecorder();
        AuthService authService = new AuthService(
                ConfiguredUserCredentialReader.singleUser(
                        "admin@example.com",
                        passwordEncoder.encode("password123"),
                        List.of(AuthRole.ADMIN)),
                passwordEncoder,
                fixedTokenService(),
                auditRecorder);

        var result = authService.login(new LoginCommand("admin@example.com", "password123", "127.0.0.1"));

        assertThat(result.user().username()).isEqualTo("admin@example.com");
        assertThat(result.token().accessToken()).isEqualTo("access-token");
        assertThat(auditRecorder.authId).isEqualTo("test-user");
        assertThat(auditRecorder.clientIp).isEqualTo("127.0.0.1");
        assertThat(auditRecorder.callCount).isEqualTo(1);
    }

    @Test
    void loginFailureDoesNotRecordAudit() {
        RecordingLoginAuditRecorder auditRecorder = new RecordingLoginAuditRecorder();
        AuthService authService = new AuthService(
                ConfiguredUserCredentialReader.singleUser(
                        "admin@example.com",
                        passwordEncoder.encode("password123"),
                        List.of(AuthRole.ADMIN)),
                passwordEncoder,
                fixedTokenService(),
                auditRecorder);

        assertThatThrownBy(() -> authService.login(new LoginCommand("admin@example.com", "wrong", "127.0.0.1")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("用户名或密码错误");
        assertThat(auditRecorder.callCount).isZero();
    }

    private AuthTokenService fixedTokenService() {
        return new AuthTokenService() {
            @Override
            public TokenIssueResult issueAccessToken(com.aurora.myblog.v2.modules.identity.domain.AuthenticatedUser user) {
                return new TokenIssueResult("access-token", Instant.parse("2030-01-01T00:00:00Z"));
            }

            @Override
            public void revoke(String accessToken) {
            }
        };
    }

    private static class RecordingLoginAuditRecorder implements LoginAuditRecorder {
        private String authId;
        private String clientIp;
        private int callCount;

        @Override
        public void recordSuccessfulLogin(String authId, String clientIp) {
            this.authId = authId;
            this.clientIp = clientIp;
            this.callCount++;
        }
    }
}
