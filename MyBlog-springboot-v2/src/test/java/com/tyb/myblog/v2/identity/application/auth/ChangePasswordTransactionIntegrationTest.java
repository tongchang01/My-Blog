package com.tyb.myblog.v2.identity.application.auth;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.identity.domain.token.RefreshTokenRecord;
import com.tyb.myblog.v2.identity.domain.token.RefreshTokenRepository;
import com.tyb.myblog.v2.identity.infrastructure.persistence.repository.MyBatisRefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 修改密码事务回滚集成测试。
 */
@ActiveProfiles("test")
@SpringBootTest
@Import(ChangePasswordTransactionIntegrationTest.FailingRepositoryConfiguration.class)
class ChangePasswordTransactionIntegrationTest {

    private static final long USER_ID = 8101L;
    private static final String OLD_PASSWORD = "old-password";
    private static final String NEW_PASSWORD = "new-password";

    @Autowired
    private ChangePasswordApplicationService service;

    @Autowired
    private FailingRefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void prepareAccount() {
        refreshTokenRepository.failRevocation(false);
        jdbcTemplate.update("DELETE FROM t_refresh_token");
        jdbcTemplate.update("DELETE FROM t_user_auth");
        jdbcTemplate.update(
                """
                        INSERT INTO t_user_auth (
                            id, username, password_hash, type,
                            token_version, deleted
                        ) VALUES (?, 'rollback-admin', ?, 1, 3, 0)
                        """,
                USER_ID,
                passwordEncoder.encode(OLD_PASSWORD));
        jdbcTemplate.update(
                """
                        INSERT INTO t_refresh_token (
                            id, user_id, token_hash, expires_at, revoked
                        ) VALUES (?, ?, ?, ?, 0)
                        """,
                9101L,
                USER_ID,
                "a".repeat(64),
                LocalDateTime.now().plusDays(1));
    }

    @Test
    void rollsBackPasswordAndTokenVersionWhenRefreshRevocationFails() {
        assertThat(AopUtils.isAopProxy(service)).isTrue();
        refreshTokenRepository.failRevocation(true);

        assertThatThrownBy(() -> service.change(
                principal(),
                new ChangePasswordCommand(
                        OLD_PASSWORD,
                        NEW_PASSWORD)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("refresh token撤销失败");

        Map<String, Object> account = jdbcTemplate.queryForMap(
                """
                        SELECT password_hash, token_version
                        FROM t_user_auth
                        WHERE id = ?
                        """,
                USER_ID);
        assertThat(passwordEncoder.matches(
                OLD_PASSWORD,
                (String) account.get("PASSWORD_HASH"))).isTrue();
        assertThat(account.get("TOKEN_VERSION")).isEqualTo(3);
        assertThat(activeRefreshTokenCount()).isEqualTo(1);
    }

    private AuthenticatedPrincipal principal() {
        return new AuthenticatedPrincipal(
                Long.toString(USER_ID),
                "rollback-admin",
                List.of("ADMIN"));
    }

    private int activeRefreshTokenCount() {
        return jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM t_refresh_token
                        WHERE user_id = ? AND revoked = 0
                        """,
                Integer.class,
                USER_ID);
    }

    /**
     * 允许测试在真实仓储写入前后注入撤销失败。
     */
    static final class FailingRefreshTokenRepository
            implements RefreshTokenRepository {

        private final RefreshTokenRepository delegate;
        private volatile boolean failRevocation;

        private FailingRefreshTokenRepository(
                RefreshTokenRepository delegate) {
            this.delegate = delegate;
        }

        void failRevocation(boolean failRevocation) {
            this.failRevocation = failRevocation;
        }

        @Override
        public void save(RefreshTokenRecord token) {
            delegate.save(token);
        }

        @Override
        public Optional<RefreshTokenRecord> findActiveForUpdate(
                String tokenHash,
                LocalDateTime now) {
            return delegate.findActiveForUpdate(tokenHash, now);
        }

        @Override
        public boolean revoke(long id) {
            return delegate.revoke(id);
        }

        @Override
        public int revokeAllByUserId(long userId) {
            if (failRevocation) {
                throw new IllegalStateException("refresh token撤销失败");
            }
            return delegate.revokeAllByUserId(userId);
        }
    }

    @TestConfiguration
    static class FailingRepositoryConfiguration {

        @Bean
        @Primary
        FailingRefreshTokenRepository failingRefreshTokenRepository(
                MyBatisRefreshTokenRepository delegate) {
            return new FailingRefreshTokenRepository(delegate);
        }
    }
}
