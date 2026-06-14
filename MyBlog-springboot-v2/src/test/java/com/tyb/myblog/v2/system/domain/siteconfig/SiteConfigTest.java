package com.tyb.myblog.v2.system.domain.siteconfig;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 站点配置领域规则测试。
 */
class SiteConfigTest {

    private static final LocalDateTime UPDATED_AT =
            LocalDateTime.of(2026, 6, 14, 12, 0);

    @Test
    void normalizesEditableFields() {
        SiteConfig config = SiteConfig.create(
                1L,
                " MyBlog ",
                " ",
                " English ",
                " 中文副标题 ",
                null,
                " ",
                "  # 关于我\n",
                "   ",
                null,
                " https://example.com/logo.png ",
                null,
                " ICP 123 ",
                " playlist_123-abc ",
                UPDATED_AT,
                1001L);

        assertThat(config.siteTitleZh()).isEqualTo("MyBlog");
        assertThat(config.siteTitleJa()).isNull();
        assertThat(config.siteTitleEn()).isEqualTo("English");
        assertThat(config.siteSubtitleZh()).isEqualTo("中文副标题");
        assertThat(config.aboutMdZh()).isEqualTo("  # 关于我\n");
        assertThat(config.aboutMdJa()).isNull();
        assertThat(config.logoUrl()).isEqualTo("https://example.com/logo.png");
        assertThat(config.icpNo()).isEqualTo("ICP 123");
        assertThat(config.spotifyPlaylistId()).isEqualTo("playlist_123-abc");
    }

    @Test
    void rejectsInvalidFixedIdAndRequiredTitle() {
        assertThatThrownBy(() -> config(2L, "MyBlog"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ID");
        assertThatThrownBy(() -> config(1L, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("中文站点标题");
        assertThatThrownBy(() -> config(1L, "x".repeat(129)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("128");
    }

    @Test
    void rejectsOversizedLocalizedText() {
        assertThatThrownBy(() -> SiteConfig.create(
                1L, "MyBlog", "x".repeat(129), null,
                null, null, null,
                null, null, null,
                null, null, null, null,
                UPDATED_AT, 1001L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("128");
        assertThatThrownBy(() -> SiteConfig.create(
                1L, "MyBlog", null, null,
                "x".repeat(256), null, null,
                null, null, null,
                null, null, null, null,
                UPDATED_AT, 1001L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("255");
        assertThatThrownBy(() -> SiteConfig.create(
                1L, "MyBlog", null, null,
                null, null, null,
                "x".repeat(50_001), null, null,
                null, null, null, null,
                UPDATED_AT, 1001L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("50000");
    }

    @Test
    void rejectsInvalidUrlsAndSpotifyIds() {
        for (String invalidUrl : new String[]{
                "/logo.png",
                "https:///logo.png",
                "javascript:alert(1)",
                "data:text/plain,test"
        }) {
            assertThatThrownBy(() -> SiteConfig.create(
                    1L, "MyBlog", null, null,
                    null, null, null,
                    null, null, null,
                    invalidUrl, null, null, null,
                    UPDATED_AT, 1001L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        for (String invalidId : new String[]{
                "contains space",
                "playlist/id",
                "x".repeat(65)
        }) {
            assertThatThrownBy(() -> SiteConfig.create(
                    1L, "MyBlog", null, null,
                    null, null, null,
                    null, null, null,
                    null, null, null, invalidId,
                    UPDATED_AT, 1001L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void fallsBackToChinesePerLocalizedField() {
        SiteConfig config = SiteConfig.create(
                1L,
                "中文标题",
                "日本語タイトル",
                null,
                "中文副标题",
                null,
                "English subtitle",
                "# 中文关于我",
                null,
                "# About",
                null,
                null,
                null,
                null,
                UPDATED_AT,
                1001L);

        assertThat(config.siteTitle(SiteLanguage.ZH)).isEqualTo("中文标题");
        assertThat(config.siteTitle(SiteLanguage.JA)).isEqualTo("日本語タイトル");
        assertThat(config.siteTitle(SiteLanguage.EN)).isEqualTo("中文标题");
        assertThat(config.siteSubtitle(SiteLanguage.JA)).isEqualTo("中文副标题");
        assertThat(config.siteSubtitle(SiteLanguage.EN)).isEqualTo("English subtitle");
        assertThat(config.aboutMd(SiteLanguage.JA)).isEqualTo("# 中文关于我");
        assertThat(config.aboutMd(SiteLanguage.EN)).isEqualTo("# About");
    }

    @Test
    void parsesOnlySupportedLanguageCodes() {
        assertThat(SiteLanguage.parse("zh")).isEqualTo(SiteLanguage.ZH);
        assertThat(SiteLanguage.parse("ja")).isEqualTo(SiteLanguage.JA);
        assertThat(SiteLanguage.parse("en")).isEqualTo(SiteLanguage.EN);
        assertThatThrownBy(() -> SiteLanguage.parse(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SiteLanguage.parse("ZH"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private SiteConfig config(long id, String title) {
        return SiteConfig.create(
                id,
                title,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                UPDATED_AT,
                1001L);
    }
}
