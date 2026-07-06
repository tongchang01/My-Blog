package com.tyb.myblog.v2.system.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 公开站点配置 HTTP 契约测试。
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class PublicSiteConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetSiteConfig() {
        jdbcTemplate.update("DELETE FROM t_site_config");
        jdbcTemplate.update("""
                INSERT INTO t_site_config (
                    id, site_title_zh, site_title_ja,
                    site_subtitle_zh, about_md_zh, started_date, deleted
                ) VALUES (
                    1, '中文标题', '日本語タイトル',
                    '中文副标题', '# 中文关于我', '2024-01-02', 0
                )
                """);
    }

    @Test
    void permitsAnonymousPublicReadWithLanguageFallback() throws Exception {
        mockMvc.perform(get("/api/public/site-config")
                        .queryParam("lang", "ja"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.siteTitle")
                        .value("日本語タイトル"))
                .andExpect(jsonPath("$.data.siteSubtitle")
                        .value("中文副标题"))
                .andExpect(jsonPath("$.data.aboutMd")
                        .value("# 中文关于我"))
                .andExpect(jsonPath("$.data.startedDate")
                        .value("2024-01-02"))
                .andExpect(jsonPath("$.data.siteTitleZh").doesNotExist())
                .andExpect(jsonPath("$.data.updatedAt").doesNotExist())
                .andExpect(jsonPath("$.data.deleted").doesNotExist());
    }

    @Test
    void rejectsMissingAndUnsupportedLanguage() throws Exception {
        mockMvc.perform(get("/api/public/site-config"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("90001"));
        mockMvc.perform(get("/api/public/site-config")
                        .queryParam("lang", "fr"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("90001"));
    }

    @Test
    void doesNotExposePostAsAnonymousEndpoint() throws Exception {
        mockMvc.perform(post("/api/public/site-config"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("10002"));
    }
}
