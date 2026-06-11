package com.tyb.myblog.v2.identity.application;

import com.tyb.myblog.v2.common.auth.token.AccessTokenIssuer;
import com.tyb.myblog.v2.common.auth.token.AccessTokenVerifier;
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
class PersistentAccessTokenVerifierTest {

    @Autowired
    private AccessTokenIssuer tokenIssuer;

    @Autowired
    private AccessTokenVerifier tokenVerifier;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearUsers() {
        jdbcTemplate.update("delete from t_refresh_token");
        jdbcTemplate.update("delete from t_user_auth");
    }

    @Test
    void acceptsTokenMatchingCurrentUserTokenVersion() {
        insertUser(1001L, 3, 0);
        String token = issueToken(1001L, 3);

        assertThat(tokenVerifier.verify(token)).isPresent();
    }

    @Test
    void rejectsTokenWithOutdatedUserTokenVersion() {
        insertUser(1001L, 4, 0);
        String token = issueToken(1001L, 3);

        assertThat(tokenVerifier.verify(token)).isEmpty();
    }

    @Test
    void rejectsTokenForMissingOrDeletedUser() {
        String missingUserToken = issueToken(1001L, 0);
        insertUser(2002L, 0, 1);
        String deletedUserToken = issueToken(2002L, 0);

        assertThat(tokenVerifier.verify(missingUserToken)).isEmpty();
        assertThat(tokenVerifier.verify(deletedUserToken)).isEmpty();
    }

    private String issueToken(long userId, int tokenVersion) {
        return tokenIssuer.issueAccessToken(
                Long.toString(userId),
                "admin",
                List.of("ADMIN"),
                tokenVersion).accessToken();
    }

    private void insertUser(long userId, int tokenVersion, int deleted) {
        jdbcTemplate.update("""
                insert into t_user_auth (
                    id, username, password_hash, type, token_version, deleted
                ) values (?, ?, ?, ?, ?, ?)
                """,
                userId,
                "admin-" + userId,
                "$2a$10$test-password-hash",
                1,
                tokenVersion,
                deleted);
    }
}
