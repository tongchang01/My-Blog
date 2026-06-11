package com.tyb.myblog.v2.identity.application;

import com.tyb.myblog.v2.identity.application.token.IssuedRefreshToken;
import com.tyb.myblog.v2.identity.application.token.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class RefreshTokenServiceTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearRefreshTokens() {
        jdbcTemplate.update("delete from t_refresh_token");
        jdbcTemplate.update("delete from t_user_auth");
    }

    /**
     * 验证 refresh token 明文只返回给调用方，数据库仅保存不可逆摘要。
     */
    @Test
    void issuesHighEntropyTokenAndStoresOnlySha256Hash() {
        IssuedRefreshToken issued = refreshTokenService.issue(1001L);

        String storedHash = jdbcTemplate.queryForObject(
                "select token_hash from t_refresh_token where user_id = 1001",
                String.class);
        Integer rawTokenCount = jdbcTemplate.queryForObject(
                "select count(*) from t_refresh_token where token_hash = ?",
                Integer.class,
                issued.token());

        assertThat(issued.token()).hasSizeGreaterThanOrEqualTo(43);
        assertThat(storedHash).isEqualTo(sha256(issued.token()));
        assertThat(rawTokenCount).isZero();
        assertThat(issued.expiresAt()).isAfter(LocalDateTime.now());
    }

    /**
     * 验证同一枚 refresh token 只能成功轮换一次，防止旧 token 重放。
     */
    @Test
    void rotatesTokenOnceAndRejectsReusingConsumedToken() {
        insertUser(1001L, 0);
        IssuedRefreshToken first = refreshTokenService.issue(1001L);

        IssuedRefreshToken second = refreshTokenService.rotate(first.token()).orElseThrow();

        assertThat(second.token()).isNotEqualTo(first.token());
        assertThat(refreshTokenService.rotate(first.token())).isEmpty();
        Integer activeCount = jdbcTemplate.queryForObject(
                "select count(*) from t_refresh_token where user_id = 1001 and revoked = 0",
                Integer.class);
        assertThat(activeCount).isEqualTo(1);
    }

    /**
     * 验证过期或已撤销的 refresh token 不能继续轮换或重复撤销。
     */
    @Test
    void rejectsExpiredOrRevokedToken() {
        insertUser(1001L, 0);
        IssuedRefreshToken expired = refreshTokenService.issue(1001L);
        jdbcTemplate.update(
                "update t_refresh_token set expires_at = ? where user_id = 1001",
                LocalDateTime.now().minusMinutes(1));

        IssuedRefreshToken revoked = refreshTokenService.issue(1001L);
        assertThat(refreshTokenService.revoke(revoked.token())).isTrue();

        assertThat(refreshTokenService.rotate(expired.token())).isEmpty();
        assertThat(refreshTokenService.rotate(revoked.token())).isEmpty();
        assertThat(refreshTokenService.revoke(revoked.token())).isFalse();
    }

    /**
     * 验证后台用户被删除后，历史 refresh token 不能再轮换出新 token。
     */
    @Test
    void rejectsRotationForDeletedUser() {
        insertUser(1001L, 0);
        IssuedRefreshToken issued = refreshTokenService.issue(1001L);
        jdbcTemplate.update("update t_user_auth set deleted = 1 where id = 1001");

        assertThat(refreshTokenService.rotate(issued.token())).isEmpty();
        assertThat(activeTokenCount(1001L)).isZero();
    }

    /**
     * 验证后台用户仍处于锁定期时，历史 refresh token 不能再轮换出新 token。
     */
    @Test
    void rejectsRotationForLockedUser() {
        insertUser(1001L, 0);
        IssuedRefreshToken issued = refreshTokenService.issue(1001L);
        jdbcTemplate.update(
                "update t_user_auth set locked_until = ? where id = 1001",
                LocalDateTime.now().plusMinutes(5));

        assertThat(refreshTokenService.rotate(issued.token())).isEmpty();
        assertThat(activeTokenCount(1001L)).isZero();
    }

    /**
     * 验证按用户整体撤销不会误伤其他后台用户的 refresh token。
     */
    @Test
    void revokesAllActiveTokensForUser() {
        insertUser(1001L, 0);
        IssuedRefreshToken first = refreshTokenService.issue(1001L);
        IssuedRefreshToken second = refreshTokenService.issue(1001L);
        refreshTokenService.issue(2002L);

        int revokedCount = refreshTokenService.revokeAllForUser(1001L);

        assertThat(revokedCount).isEqualTo(2);
        assertThat(refreshTokenService.rotate(first.token())).isEmpty();
        assertThat(refreshTokenService.rotate(second.token())).isEmpty();
        Integer otherUserActiveCount = jdbcTemplate.queryForObject(
                "select count(*) from t_refresh_token where user_id = 2002 and revoked = 0",
                Integer.class);
        assertThat(otherUserActiveCount).isEqualTo(1);
    }

    private void insertUser(long userId, int deleted) {
        jdbcTemplate.update("""
                insert into t_user_auth (
                    id, username, password_hash, type, token_version, deleted
                ) values (?, ?, ?, ?, 0, ?)
                """,
                userId,
                "admin-" + userId,
                "$2a$10$test-password-hash",
                1,
                deleted);
    }

    private int activeTokenCount(long userId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from t_refresh_token where user_id = ? and revoked = 0",
                Integer.class,
                userId);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
