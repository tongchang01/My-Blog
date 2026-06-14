package com.tyb.myblog.v2.identity.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 修改密码完整 HTTP 会话验收测试。
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ChangePasswordIntegrationTest {

    private static final String OLD_PASSWORD = "old-password";
    private static final String NEW_PASSWORD = "new-password";
    private static final String OTHER_PASSWORD = "other-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM t_refresh_token");
        jdbcTemplate.update("DELETE FROM t_user_auth");
    }

    @Test
    void changesPasswordAndInvalidatesAllSessionsWithoutAffectingAnotherAccount()
            throws Exception {
        insertAccount(1001L, "admin-one", AccountType.ADMIN, OLD_PASSWORD, 3);
        insertAccount(2002L, "admin-two", AccountType.ADMIN, OTHER_PASSWORD, 5);

        TokenView firstSession = login("admin-one", OLD_PASSWORD);
        TokenView secondSession = login("admin-one", OLD_PASSWORD);
        TokenView otherSession = login("admin-two", OTHER_PASSWORD);

        changePassword(
                firstSession.accessToken(),
                OLD_PASSWORD,
                NEW_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").isEmpty());

        assertProtectedRequestUnauthorized(firstSession.accessToken());
        assertProtectedRequestUnauthorized(secondSession.accessToken());
        assertRefreshUnauthorized(firstSession.refreshToken());
        assertRefreshUnauthorized(secondSession.refreshToken());
        assertLoginUnauthorized("admin-one", OLD_PASSWORD);
        assertThat(login("admin-one", NEW_PASSWORD).accessToken())
                .isNotBlank();
        assertProtectedRequestSucceeds(otherSession.accessToken());
        assertThat(currentTokenVersion(1001L)).isEqualTo(4);
        assertThat(currentTokenVersion(2002L)).isEqualTo(5);
    }

    @Test
    void keepsSessionAndPasswordWhenCurrentPasswordIsWrong()
            throws Exception {
        insertAccount(3001L, "wrong-current", AccountType.ADMIN, OLD_PASSWORD, 2);
        TokenView session = login("wrong-current", OLD_PASSWORD);

        changePassword(
                session.accessToken(),
                "wrong-password",
                NEW_PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("10001"));

        assertProtectedRequestSucceeds(session.accessToken());
        assertThat(refresh(session.refreshToken()).accessToken()).isNotBlank();
        assertThat(login("wrong-current", OLD_PASSWORD).accessToken())
                .isNotBlank();
        assertThat(currentTokenVersion(3001L)).isEqualTo(2);
    }

    @Test
    void rejectsSamePasswordDemoAndUnauthenticatedRequest()
            throws Exception {
        insertAccount(4001L, "same-password", AccountType.ADMIN, OLD_PASSWORD, 0);
        insertAccount(4002L, "demo", AccountType.DEMO, OLD_PASSWORD, 0);
        TokenView admin = login("same-password", OLD_PASSWORD);
        TokenView demo = login("demo", OLD_PASSWORD);

        changePassword(
                admin.accessToken(),
                OLD_PASSWORD,
                OLD_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("90001"));
        changePassword(
                demo.accessToken(),
                OLD_PASSWORD,
                NEW_PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("10003"));
        changePassword(null, OLD_PASSWORD, NEW_PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("10002"));

        assertProtectedRequestSucceeds(admin.accessToken());
        assertThat(currentTokenVersion(4001L)).isZero();
        assertThat(currentTokenVersion(4002L)).isZero();
    }

    private TokenView login(String username, String password)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of(
                                        "username", username,
                                        "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andReturn();
        return tokenView(result);
    }

    private void assertLoginUnauthorized(String username, String password)
            throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of(
                                        "username", username,
                                        "password", password))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("10001"));
    }

    private TokenView refresh(String refreshToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andReturn();
        return tokenView(result);
    }

    private void assertRefreshUnauthorized(String refreshToken)
            throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("10002"));
    }

    private ResultActions changePassword(
            String accessToken,
            String currentPassword,
            String newPassword
    ) throws Exception {
        var request = put("/api/auth/me/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of(
                                "currentPassword", currentPassword,
                                "newPassword", newPassword)));
        if (accessToken != null) {
            request.header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + accessToken);
        }
        return mockMvc.perform(request);
    }

    private void assertProtectedRequestSucceeds(String accessToken)
            throws Exception {
        mockMvc.perform(get("/api/admin/security-probe")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    private void assertProtectedRequestUnauthorized(String accessToken)
            throws Exception {
        mockMvc.perform(get("/api/admin/security-probe")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + accessToken))
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

    private void insertAccount(
            long id,
            String username,
            AccountType type,
            String password,
            int tokenVersion
    ) {
        jdbcTemplate.update(
                """
                        INSERT INTO t_user_auth (
                            id, username, password_hash, type,
                            token_version, deleted
                        ) VALUES (?, ?, ?, ?, ?, 0)
                        """,
                id,
                username,
                passwordEncoder.encode(password),
                type.databaseValue(),
                tokenVersion);
    }

    private int currentTokenVersion(long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT token_version FROM t_user_auth WHERE id = ?",
                Integer.class,
                userId);
    }

    private record TokenView(
            String accessToken,
            String refreshToken
    ) {
    }
}
