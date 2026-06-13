package com.tyb.myblog.v2.identity.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyb.myblog.v2.identity.application.token.IssuedRefreshToken;
import com.tyb.myblog.v2.identity.application.token.RefreshTokenService;
import com.tyb.myblog.v2.identity.domain.account.AccountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 后台认证会话从登录到全端退出的完整 HTTP 验收测试。
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AuthSessionIntegrationTest {

    private static final String PASSWORD = "correct-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM t_refresh_token");
        jdbcTemplate.update("DELETE FROM t_user_auth");
    }

    @Test
    void shouldRefreshAndLogoutWithoutAffectingAnotherAccount()
            throws Exception {
        insertAccount(1001L, "admin-one", AccountType.ADMIN, 3, null, 0);
        insertAccount(2002L, "admin-two", AccountType.ADMIN, 5, null, 0);

        TokenView firstLogin = login("admin-one");
        TokenView otherLogin = login("admin-two");
        TokenView refreshed = refresh(firstLogin.refreshToken());

        assertInvalidRefresh(firstLogin.refreshToken());
        assertAdminProbeSucceeds(refreshed.accessToken());

        mockMvc.perform(post("/api/auth/logout")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(refreshed.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").isEmpty());

        assertAdminProbeUnauthorized(firstLogin.accessToken());
        assertAdminProbeUnauthorized(refreshed.accessToken());
        assertInvalidRefresh(refreshed.refreshToken());

        assertAdminProbeSucceeds(otherLogin.accessToken());
        assertThat(refresh(otherLogin.refreshToken()).refreshToken())
                .isNotBlank();
        assertThat(currentTokenVersion(1001L)).isEqualTo(4);
        assertThat(currentTokenVersion(2002L)).isEqualTo(5);
    }

    @Test
    void shouldRejectRefreshForLockedDeletedAndGuestAccounts()
            throws Exception {
        insertAccount(
                3001L,
                "locked",
                AccountType.ADMIN,
                0,
                LocalDateTime.now().plusMinutes(5),
                0);
        insertAccount(
                3002L,
                "deleted",
                AccountType.ADMIN,
                0,
                null,
                1);
        insertAccount(
                3003L,
                "guest",
                AccountType.GUEST,
                0,
                null,
                0);
        IssuedRefreshToken locked = refreshTokenService.issue(3001L);
        IssuedRefreshToken deleted = refreshTokenService.issue(3002L);
        IssuedRefreshToken guest = refreshTokenService.issue(3003L);

        assertInvalidRefresh(locked.token());
        assertInvalidRefresh(deleted.token());
        assertInvalidRefresh(guest.token());
        assertThat(activeRefreshTokenCount(3001L)).isZero();
        assertThat(activeRefreshTokenCount(3002L)).isZero();
        assertThat(activeRefreshTokenCount(3003L)).isZero();
    }

    private TokenView login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of(
                                        "username", username,
                                        "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andReturn();
        return tokenView(result);
    }

    private TokenView refresh(String refreshToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of(
                                        "refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andReturn();
        return tokenView(result);
    }

    private void assertInvalidRefresh(String refreshToken)
            throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of(
                                        "refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("10002"));
    }

    private void assertAdminProbeSucceeds(String accessToken)
            throws Exception {
        mockMvc.perform(get("/api/admin/security-probe")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(accessToken)))
                .andExpect(status().isOk());
    }

    private void assertAdminProbeUnauthorized(String accessToken)
            throws Exception {
        mockMvc.perform(get("/api/admin/security-probe")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(accessToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("10002"));
    }

    private TokenView tokenView(MvcResult result) throws Exception {
        JsonNode data = objectMapper.readTree(
                        result.getResponse().getContentAsString())
                .path("data");
        return new TokenView(
                data.path("accessToken").asText(),
                data.path("refreshToken").asText());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void insertAccount(
            long id,
            String username,
            AccountType type,
            int tokenVersion,
            LocalDateTime lockedUntil,
            int deleted
    ) {
        jdbcTemplate.update("""
                        INSERT INTO t_user_auth (
                            id, username, password_hash, type,
                            token_version, locked_until, deleted
                        ) VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                id,
                username,
                passwordEncoder.encode(PASSWORD),
                type.databaseValue(),
                tokenVersion,
                lockedUntil,
                deleted);
    }

    private int currentTokenVersion(long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT token_version FROM t_user_auth WHERE id = ?",
                Integer.class,
                userId);
    }

    private int activeRefreshTokenCount(long userId) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM t_refresh_token
                        WHERE user_id = ? AND revoked = 0
                        """,
                Integer.class,
                userId);
    }

    private record TokenView(
            String accessToken,
            String refreshToken
    ) {
    }
}
