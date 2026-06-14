package com.tyb.myblog.v2.system.web;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.GlobalExceptionHandler;
import com.tyb.myblog.v2.system.application.siteconfig.AdminSiteConfigQueryService;
import com.tyb.myblog.v2.system.application.siteconfig.AdminSiteConfigResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 后台站点配置 HTTP 接口测试。
 */
@WebMvcTest(AdminSiteConfigController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminSiteConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminSiteConfigQueryService queryService;

    private AuthenticatedPrincipal principal;

    @BeforeEach
    void setUp() {
        principal = new AuthenticatedPrincipal(
                "1001",
                "admin",
                List.of("ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsCompleteConfigurationAndPassesPrincipalUnchanged()
            throws Exception {
        when(queryService.query(principal)).thenReturn(result());

        mockMvc.perform(get("/api/admin/site-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.siteTitleZh").value("中文标题"))
                .andExpect(jsonPath("$.data.siteTitleJa").value("日本語タイトル"))
                .andExpect(jsonPath("$.data.siteTitleEn").value("English title"))
                .andExpect(jsonPath("$.data.siteSubtitleZh").value("中文副标题"))
                .andExpect(jsonPath("$.data.siteSubtitleJa").value("日本語サブ标题"))
                .andExpect(jsonPath("$.data.siteSubtitleEn").value("English subtitle"))
                .andExpect(jsonPath("$.data.aboutMdZh").value("# 中文关于我"))
                .andExpect(jsonPath("$.data.aboutMdJa").value("# 私について"))
                .andExpect(jsonPath("$.data.aboutMdEn").value("# About"))
                .andExpect(jsonPath("$.data.logoUrl").value("https://example.com/logo.png"))
                .andExpect(jsonPath("$.data.faviconUrl").value("https://example.com/favicon.ico"))
                .andExpect(jsonPath("$.data.icpNo").value("京ICP备12345678号"))
                .andExpect(jsonPath("$.data.spotifyPlaylistId").value("playlist_123"))
                .andExpect(jsonPath("$.data.updatedAt").value("2026-06-14T12:00:00"))
                .andExpect(jsonPath("$.data.updatedBy").value(1001));

        verify(queryService).query(principal);
    }

    private AdminSiteConfigResult result() {
        return new AdminSiteConfigResult(
                "中文标题",
                "日本語タイトル",
                "English title",
                "中文副标题",
                "日本語サブ标题",
                "English subtitle",
                "# 中文关于我",
                "# 私について",
                "# About",
                "https://example.com/logo.png",
                "https://example.com/favicon.ico",
                "京ICP备12345678号",
                "playlist_123",
                LocalDateTime.of(2026, 6, 14, 12, 0),
                1001L);
    }
}
