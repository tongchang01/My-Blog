package com.tyb.myblog.v2.common.security;

import com.tyb.myblog.v2.common.security.auth.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class JwtAuthenticationFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService tokenService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void authenticatesValidBearerTokenAndRejectsInvalidToken() throws Exception {
        jdbcTemplate.update("delete from t_refresh_token");
        jdbcTemplate.update("delete from t_user_auth");
        jdbcTemplate.update("""
                insert into t_user_auth (
                    id, username, password_hash, type, token_version, deleted
                ) values (?, ?, ?, ?, ?, ?)
                """,
                1001L,
                "admin",
                "$2a$10$test-password-hash",
                1,
                0,
                0);
        String token = tokenService
                .issueAccessToken("1001", "admin@example.com", List.of("ADMIN"), 0)
                .accessToken();

        mockMvc.perform(get("/api/admin/security-probe").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        token = token + "-invalid";

        mockMvc.perform(get("/api/admin/security-probe").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("10002"));
    }
}
