package com.aurora.myblog.v2.modules.identity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetLoginAuditFields() {
        jdbcTemplate.update("update t_user_auth set last_login_time = null, ip_address = null, ip_source = null");
    }

    @Test
    void logsInWithDatabaseCredentialWithoutExistingAuthentication() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "203.0.113.10, 198.51.100.20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin@163.com","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.username").value("admin@163.com"))
                .andExpect(jsonPath("$.data.user.roles[0]").value("ADMIN"));

        AuditRow row = loadAuditRow("1");
        assertThat(row.lastLoginTime()).isNotNull();
        assertThat(row.ipAddress()).isEqualTo("203.0.113.10");
        assertThat(row.ipSource()).isNull();
    }

    @Test
    void rejectsInvalidCredentialWithUnauthorizedEnvelope() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "203.0.113.10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin@example.com","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));

        AuditRow row = loadAuditRow("1");
        assertThat(row.lastLoginTime()).isNull();
        assertThat(row.ipAddress()).isNull();
    }

    @Test
    void loginFailureDoesNotRevealAccountExistence() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"missing@example.com","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void rejectsDisabledUserWithSameBadCredentialResponse() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"disabled@163.com","password":"password123"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void meRequiresBearerToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void meReturnsCurrentDatabaseUserProfile() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin@163.com","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = com.jayway.jsonpath.JsonPath.read(response, "$.data.accessToken");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("1"))
                .andExpect(jsonPath("$.data.userInfoId").value("1"))
                .andExpect(jsonPath("$.data.username").value("admin@163.com"))
                .andExpect(jsonPath("$.data.nickname").value("管理员"))
                .andExpect(jsonPath("$.data.avatar").value(""))
                .andExpect(jsonPath("$.data.email").value("admin@163.com"))
                .andExpect(jsonPath("$.data.roles[0]").value("ADMIN"));
    }

    private AuditRow loadAuditRow(String authId) {
        return jdbcTemplate.queryForObject("""
                        select last_login_time, ip_address, ip_source
                        from t_user_auth
                        where id = ?
                        """,
                (rs, rowNum) -> new AuditRow(
                        rs.getTimestamp("last_login_time"),
                        rs.getString("ip_address"),
                        rs.getString("ip_source")),
                authId);
    }

    private record AuditRow(java.sql.Timestamp lastLoginTime, String ipAddress, String ipSource) {
    }
}
