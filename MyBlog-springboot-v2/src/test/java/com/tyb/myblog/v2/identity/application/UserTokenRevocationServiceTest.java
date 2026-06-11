package com.tyb.myblog.v2.identity.application;

import com.tyb.myblog.v2.common.auth.token.AccessTokenIssuer;
import com.tyb.myblog.v2.common.auth.token.AccessTokenVerifier;
import com.tyb.myblog.v2.identity.application.token.IssuedRefreshToken;
import com.tyb.myblog.v2.identity.application.token.RefreshTokenService;
import com.tyb.myblog.v2.identity.application.token.UserTokenRevocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class UserTokenRevocationServiceTest {

    @Autowired
    private UserTokenRevocationService revocationService;

    @Autowired
    private AccessTokenIssuer accessTokenIssuer;

    @Autowired
    private AccessTokenVerifier accessTokenVerifier;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearTokenData() {
        jdbcTemplate.update("delete from t_refresh_token");
        jdbcTemplate.update("delete from t_user_auth");
    }

    @Test
    void revokesAllAccessAndRefreshTokensForUser() {
        insertUser(1001L, 3);
        insertUser(2002L, 1);
        String accessToken = issueAccessToken(1001L, 3);
        IssuedRefreshToken firstRefreshToken = refreshTokenService.issue(1001L);
        IssuedRefreshToken secondRefreshToken = refreshTokenService.issue(1001L);
        IssuedRefreshToken otherUserRefreshToken = refreshTokenService.issue(2002L);

        boolean revoked = revocationService.revokeAll(1001L);

        assertThat(revoked).isTrue();
        assertThat(accessTokenVerifier.verify(accessToken)).isEmpty();
        assertThat(refreshTokenService.rotate(firstRefreshToken.token())).isEmpty();
        assertThat(refreshTokenService.rotate(secondRefreshToken.token())).isEmpty();
        assertThat(refreshTokenService.rotate(otherUserRefreshToken.token())).isPresent();
        assertThat(currentTokenVersion(1001L)).isEqualTo(4);
        assertThat(currentTokenVersion(2002L)).isEqualTo(1);
    }

    private String issueAccessToken(long userId, int tokenVersion) {
        return accessTokenIssuer.issueAccessToken(
                Long.toString(userId),
                "admin-" + userId,
                List.of("ADMIN"),
                tokenVersion).accessToken();
    }

    private void insertUser(long userId, int tokenVersion) {
        jdbcTemplate.update("""
                insert into t_user_auth (
                    id, username, password_hash, type, token_version, deleted
                ) values (?, ?, ?, ?, ?, 0)
                """,
                userId,
                "admin-" + userId,
                "$2a$10$test-password-hash",
                1,
                tokenVersion);
    }

    private int currentTokenVersion(long userId) {
        return jdbcTemplate.queryForObject(
                "select token_version from t_user_auth where id = ?",
                Integer.class,
                userId);
    }
}
