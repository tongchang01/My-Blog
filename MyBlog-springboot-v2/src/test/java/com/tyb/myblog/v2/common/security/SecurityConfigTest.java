package com.tyb.myblog.v2.common.security;

import com.tyb.myblog.v2.common.security.auth.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService tokenService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void clearUsers() {
        jdbcTemplate.update("delete from t_attachment");
        jdbcTemplate.update("delete from t_refresh_token");
        jdbcTemplate.update("delete from t_user_info");
        jdbcTemplate.update("delete from t_user_auth");
        jdbcTemplate.update("delete from t_site_config");
        jdbcTemplate.update("""
                insert into t_site_config (
                    id, site_title_zh, deleted
                ) values (1, 'MyBlog', 0)
                """);
    }

    @Test
    void permitsOnlyConfiguredPublicProbe() throws Exception {
        mockMvc.perform(get("/api/public/security-probe"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsPublicPathWhenHttpMethodIsNotConfigured() throws Exception {
        mockMvc.perform(post("/api/public/security-probe"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("10002"));
    }

    @Test
    void rejectsUnconfiguredApiRoute() throws Exception {
        mockMvc.perform(get("/api/admin/security-probe"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("10002"));
    }

    @Test
    void permitsConfiguredLoginPostWithoutAccessToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"missing","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("10001"));
    }

    @Test
    void doesNotPermitUnconfiguredLoginGet() throws Exception {
        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("10002"));
    }

    @Test
    void permitsConfiguredRefreshPostWithoutAccessToken() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"invalid"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("10002"));
    }

    @Test
    void doesNotPermitRefreshGet() throws Exception {
        mockMvc.perform(get("/api/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("10002"));
    }

    @Test
    void requiresAccessTokenForLogout() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("10002"));
    }

    @Test
    void returnsForbiddenWhenRoleIsInsufficient() throws Exception {
        String token = token(1001L, "demo", 2, "DEMO");

        mockMvc.perform(get("/api/admin/security-probe").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("10003"));
    }

    @Test
    void permitsOnlyAdminToUploadAttachment() throws Exception {
        String adminToken = token(1001L, "admin", 1, "ADMIN");
        String demoToken = token(1002L, "demo", 2, "DEMO");

        mockMvc.perform(multipart("/api/admin/attachments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("10002"));
        mockMvc.perform(multipart("/api/admin/attachments")
                        .header("Authorization", "Bearer " + demoToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("10003"));
        mockMvc.perform(multipart("/api/admin/attachments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("90001"));
    }

    @Test
    void permitsAdminToPatchCurrentProfile() throws Exception {
        String token = token(1001L, "admin", 1, "ADMIN");

        mockMvc.perform(patch("/api/auth/me/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"Admin"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void forbidsDemoFromPatchingCurrentProfile() throws Exception {
        String token = token(1002L, "demo", 2, "DEMO");

        mockMvc.perform(patch("/api/auth/me/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"Demo"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("10003"));
    }

    @Test
    void permitsAdminAndDemoToReadCurrentProfile() throws Exception {
        String adminToken = token(1001L, "admin", 1, "ADMIN");
        String demoToken = token(1002L, "demo", 2, "DEMO");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + demoToken))
                .andExpect(status().isOk());
    }

    @Test
    void permitsAdminAndDemoToReadSiteConfig() throws Exception {
        String adminToken = token(1001L, "admin", 1, "ADMIN");
        String demoToken = token(1002L, "demo", 2, "DEMO");

        mockMvc.perform(get("/api/admin/site-config")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/site-config")
                        .header("Authorization", "Bearer " + demoToken))
                .andExpect(status().isOk());
    }

    @Test
    void requiresAuthenticationForAdminSiteConfigRead() throws Exception {
        mockMvc.perform(get("/api/admin/site-config"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("10002"));
    }

    @Test
    void permitsOnlyAdminToUpdateSiteConfig() throws Exception {
        String adminToken = token(1001L, "admin", 1, "ADMIN");
        String demoToken = token(1002L, "demo", 2, "DEMO");

        mockMvc.perform(put("/api/admin/site-config")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(siteConfigRequest()))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/admin/site-config")
                        .header("Authorization", "Bearer " + demoToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(siteConfigRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("10003"));
        mockMvc.perform(put("/api/admin/site-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(siteConfigRequest()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("10002"));
    }

    @Test
    void doesNotExtendDemoReadPermissionToOtherMethods() throws Exception {
        String demoToken = token(1002L, "demo", 2, "DEMO");

        mockMvc.perform(post("/api/admin/site-config")
                        .header("Authorization", "Bearer " + demoToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/admin/site-config")
                        .header("Authorization", "Bearer " + demoToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/admin/site-config")
                        .header("Authorization", "Bearer " + demoToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void requiresAuthenticationForCurrentProfileEndpoints() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("10002"));
        mockMvc.perform(patch("/api/auth/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("10002"));
    }

    @Test
    void permitsOnlyAdminToChangeCurrentPassword() throws Exception {
        String adminToken = token(1001L, "admin", 1, "ADMIN");
        String demoToken = token(1002L, "demo", 2, "DEMO");
        String request = """
                {
                  "currentPassword":"old-password",
                  "newPassword":"new-password"
                }
                """;

        mockMvc.perform(put("/api/auth/me/password")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/auth/me/password")
                        .header("Authorization", "Bearer " + demoToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("10003"));
    }

    @Test
    void requiresAuthenticationAndExactMethodForChangePassword()
            throws Exception {
        mockMvc.perform(put("/api/auth/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword":"old-password",
                                  "newPassword":"new-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("10002"));

        String demoToken = token(1002L, "demo", 2, "DEMO");
        mockMvc.perform(post("/api/auth/me/password")
                        .header("Authorization", "Bearer " + demoToken))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(patch("/api/auth/me/password")
                        .header("Authorization", "Bearer " + demoToken))
                .andExpect(status().isMethodNotAllowed());
    }

    private String token(
            long id,
            String username,
            int type,
            String role
    ) {
        jdbcTemplate.update("""
                insert into t_user_auth (
                    id, username, password_hash, type, token_version, deleted
                ) values (?, ?, ?, ?, ?, ?)
                """,
                id,
                username,
                passwordEncoder.encode("old-password"),
                type,
                0,
                0);
        jdbcTemplate.update("""
                insert into t_user_info (
                    user_id, nickname, deleted
                ) values (?, ?, 0)
                """,
                id,
                username);
        return tokenService
                .issueAccessToken(
                        Long.toString(id),
                        username,
                        List.of(role),
                        0)
                .accessToken();
    }

    private String siteConfigRequest() {
        return """
                {
                  "siteTitleZh":"MyBlog",
                  "siteTitleJa":null,
                  "siteTitleEn":"My Blog",
                  "siteSubtitleZh":"中文副标题",
                  "siteSubtitleJa":null,
                  "siteSubtitleEn":null,
                  "aboutMdZh":"# 关于我",
                  "aboutMdJa":null,
                  "aboutMdEn":null,
                  "logoUrl":"https://example.com/logo.png",
                  "faviconUrl":null,
                  "icpNo":null,
                  "spotifyPlaylistId":"playlist_123"
                }
                """;
    }
}
