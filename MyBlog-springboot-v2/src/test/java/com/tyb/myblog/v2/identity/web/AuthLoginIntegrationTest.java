package com.tyb.myblog.v2.identity.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyb.myblog.v2.common.auth.token.AccessTokenDecoder;
import com.tyb.myblog.v2.common.auth.token.TokenClaims;
import com.tyb.myblog.v2.identity.domain.account.AccountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 后台登录完整 HTTP 链路集成测试。
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AuthLoginIntegrationTest {

    private static final String CORRECT_PASSWORD = "correct-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccessTokenDecoder accessTokenDecoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("delete from t_refresh_token");
        jdbcTemplate.update("delete from t_user_auth");
    }

    @Test
    void logsInAdminAndReturnsDecodableTokens() throws Exception {
        insertAccount(1001L, "admin", AccountType.ADMIN, 3, 4, LocalDateTime.now().minusMinutes(1));

        MvcResult mvcResult = performLogin(
                "203.0.113.20",
                " Admin ",
                CORRECT_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.accessExpiresIn").value(900))
                .andExpect(jsonPath("$.data.refreshExpiresIn").value(604800))
                .andReturn();

        JsonNode data = responseData(mvcResult);
        TokenClaims claims = accessTokenDecoder.decode(data.path("accessToken").asText())
                .orElseThrow();
        assertThat(claims.userId()).isEqualTo("1001");
        assertThat(claims.username()).isEqualTo("admin");
        assertThat(claims.roles()).containsExactly("ADMIN");
        assertThat(claims.tokenVersion()).isEqualTo(3);
        assertThat(data.path("refreshToken").asText()).isNotBlank();
        assertThat(refreshTokenCount(1001L)).isEqualTo(1);
        assertThat(readLoginState(1001L))
                .isEqualTo(new LoginState("203.0.113.20", 0, null));
    }

    @Test
    void logsInDemoWithDemoRoleOnly() throws Exception {
        insertAccount(1002L, "demo", AccountType.DEMO, 2, 0, null);

        MvcResult mvcResult = performLogin(
                "203.0.113.21",
                "demo",
                CORRECT_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andReturn();

        TokenClaims claims = accessTokenDecoder.decode(
                        responseData(mvcResult).path("accessToken").asText())
                .orElseThrow();
        assertThat(claims.roles()).containsExactly("DEMO");
    }

    @Test
    void rejectsUnknownGuestAndLockedAccountsWithoutLeakingState() throws Exception {
        insertAccount(1003L, "guest", AccountType.GUEST, 0, 0, null);
        insertAccount(
                1004L,
                "locked",
                AccountType.ADMIN,
                0,
                5,
                LocalDateTime.now().plusMinutes(5));

        assertBadCredentials("203.0.113.30", "missing", CORRECT_PASSWORD);
        assertBadCredentials("203.0.113.31", "guest", CORRECT_PASSWORD);
        assertBadCredentials("203.0.113.32", "locked", CORRECT_PASSWORD);

        assertThat(readLoginState(1003L).loginFailCount()).isZero();
        assertThat(readLoginState(1004L).loginFailCount()).isEqualTo(5);
    }

    @Test
    void rateLimitsSixthFailureBeforePersistentStateChangesAgain() throws Exception {
        String username = uniqueUsername("limit");
        String clientIp = "198.51.100.40";
        insertAccount(1005L, username, AccountType.ADMIN, 0, 0, null);

        for (int attempt = 1; attempt <= 5; attempt++) {
            assertBadCredentials(clientIp, username, "wrong-password");
        }
        LoginState afterFifthFailure = readLoginState(1005L);
        // 数据库锁定策略在达到阈值时开启新周期，因此计数归零并写入锁定截止时间。
        assertThat(afterFifthFailure.loginFailCount()).isZero();
        assertThat(afterFifthFailure.lockedUntil()).isNotNull();

        performLogin(clientIp, username, "wrong-password")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("90002"));

        assertThat(readLoginState(1005L)).isEqualTo(afterFifthFailure);
    }

    @Test
    void successfulLoginResetsCurrentRateLimitCycle() throws Exception {
        String username = uniqueUsername("reset");
        String clientIp = "198.51.100.41";
        insertAccount(1006L, username, AccountType.ADMIN, 0, 0, null);

        for (int attempt = 1; attempt <= 4; attempt++) {
            assertBadCredentials(clientIp, username, "wrong-password");
        }
        performLogin(clientIp, username, CORRECT_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));

        for (int attempt = 1; attempt <= 5; attempt++) {
            assertBadCredentials(clientIp, username, "wrong-password");
        }
        performLogin(clientIp, username, "wrong-password")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("90002"));
    }

    private org.springframework.test.web.servlet.ResultActions performLogin(
            String clientIp,
            String username,
            String password
    ) throws Exception {
        String requestBody = objectMapper.writeValueAsString(new LoginRequest(username, password));
        return mockMvc.perform(post("/api/auth/login")
                .with(request -> {
                    request.setRemoteAddr(clientIp);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));
    }

    private void assertBadCredentials(
            String clientIp,
            String username,
            String password
    ) throws Exception {
        performLogin(clientIp, username, password)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("10001"));
    }

    private JsonNode responseData(MvcResult mvcResult) throws Exception {
        return objectMapper.readTree(mvcResult.getResponse().getContentAsString())
                .path("data");
    }

    private void insertAccount(
            long id,
            String username,
            AccountType type,
            int tokenVersion,
            int loginFailCount,
            LocalDateTime lockedUntil
    ) {
        jdbcTemplate.update("""
                insert into t_user_auth (
                    id, username, password_hash, type, token_version,
                    login_fail_count, locked_until, deleted
                ) values (?, ?, ?, ?, ?, ?, ?, 0)
                """,
                id,
                username,
                passwordEncoder.encode(CORRECT_PASSWORD),
                type.databaseValue(),
                tokenVersion,
                loginFailCount,
                lockedUntil);
    }

    private int refreshTokenCount(long userId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from t_refresh_token where user_id = ?",
                Integer.class,
                userId);
    }

    private LoginState readLoginState(long userId) {
        return jdbcTemplate.queryForObject(
                """
                        select last_login_ip, login_fail_count, locked_until
                        from t_user_auth
                        where id = ?
                        """,
                (resultSet, rowNumber) -> new LoginState(
                        resultSet.getString("last_login_ip"),
                        resultSet.getInt("login_fail_count"),
                        resultSet.getObject("locked_until", LocalDateTime.class)),
                userId);
    }

    private String uniqueUsername(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private record LoginState(
            String lastLoginIp,
            int loginFailCount,
            LocalDateTime lockedUntil
    ) {
    }
}
