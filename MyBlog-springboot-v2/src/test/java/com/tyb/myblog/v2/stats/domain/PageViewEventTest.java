package com.tyb.myblog.v2.stats.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageViewEventTest {

    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 6, 18, 12, 0);

    @Test
    void acceptsArticleAndGeneralPageEvents() {
        PageViewEvent article = PageViewEvent.create(
                100L,
                StatsLanguage.ZH,
                "a".repeat(64),
                "https://example.com/posts/100",
                NOW);
        PageViewEvent general = PageViewEvent.create(
                null,
                StatsLanguage.EN,
                "b".repeat(64),
                null,
                NOW);

        assertThat(article.articleId()).isEqualTo(100L);
        assertThat(article.language()).isEqualTo(StatsLanguage.ZH);
        assertThat(general.articleId()).isNull();
    }

    @Test
    void parsesSupportedLanguageCodes() {
        assertThat(StatsLanguage.fromCode("zh"))
                .isEqualTo(StatsLanguage.ZH);
        assertThat(StatsLanguage.fromCode("ja"))
                .isEqualTo(StatsLanguage.JA);
        assertThat(StatsLanguage.fromCode("en"))
                .isEqualTo(StatsLanguage.EN);
    }

    @Test
    void rejectsInvalidStoredValues() {
        assertThatThrownBy(() -> PageViewEvent.create(
                0L,
                StatsLanguage.ZH,
                "a".repeat(64),
                null,
                NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("文章 ID");
        assertThatThrownBy(() -> PageViewEvent.create(
                null,
                StatsLanguage.ZH,
                "short",
                null,
                NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("访客标识");
        assertThatThrownBy(() -> PageViewEvent.create(
                null,
                StatsLanguage.ZH,
                "a".repeat(64),
                "r".repeat(513),
                NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("来源地址");
        assertThatThrownBy(() -> StatsLanguage.fromCode("fr"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("语言");
    }
}
