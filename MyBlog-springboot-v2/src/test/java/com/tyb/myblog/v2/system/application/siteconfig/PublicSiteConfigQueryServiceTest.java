package com.tyb.myblog.v2.system.application.siteconfig;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.system.domain.siteconfig.SiteConfig;
import com.tyb.myblog.v2.system.domain.siteconfig.SiteConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * 公开站点配置查询服务测试。
 */
@ExtendWith(MockitoExtension.class)
class PublicSiteConfigQueryServiceTest {

    @Mock
    private SiteConfigRepository repository;

    private PublicSiteConfigQueryService service;

    @BeforeEach
    void setUp() {
        service = new PublicSiteConfigQueryService(repository);
    }

    @Test
    void returnsRequestedLanguageWithFieldLevelChineseFallback() {
        when(repository.findActive()).thenReturn(Optional.of(siteConfig()));

        PublicSiteConfigResult result = service.query("ja");

        assertThat(result.siteTitle()).isEqualTo("日本語タイトル");
        assertThat(result.siteSubtitle()).isEqualTo("中文副标题");
        assertThat(result.aboutMd()).isEqualTo("# 中文关于我");
        assertThat(result.logoUrl()).isEqualTo("https://example.com/logo.png");
    }

    @Test
    void returnsChineseAndEnglishValues() {
        when(repository.findActive()).thenReturn(Optional.of(siteConfig()));

        PublicSiteConfigResult chinese = service.query("zh");
        PublicSiteConfigResult english = service.query("en");

        assertThat(chinese.siteTitle()).isEqualTo("中文标题");
        assertThat(chinese.aboutMd()).isEqualTo("# 中文关于我");
        assertThat(english.siteTitle()).isEqualTo("中文标题");
        assertThat(english.siteSubtitle()).isEqualTo("English subtitle");
        assertThat(english.aboutMd()).isEqualTo("# About");
    }

    @Test
    void rejectsMissingOrUnsupportedLanguage() {
        for (String language : new String[]{null, "", "ZH", "fr"}) {
            assertThatThrownBy(() -> service.query(language))
                    .isInstanceOfSatisfying(
                            ApiException.class,
                            exception -> assertThat(exception.code())
                                    .isEqualTo(ApiErrorCode.VALIDATION_ERROR));
        }
    }

    @Test
    void returnsInternalErrorWhenFixedConfigurationIsMissing() {
        when(repository.findActive()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.query("zh"))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(ApiErrorCode.INTERNAL_ERROR));
    }

    private SiteConfig siteConfig() {
        return SiteConfig.create(
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
                "https://example.com/logo.png",
                null,
                null,
                "playlist_123",
                LocalDateTime.of(2026, 6, 14, 12, 0),
                1001L);
    }
}
