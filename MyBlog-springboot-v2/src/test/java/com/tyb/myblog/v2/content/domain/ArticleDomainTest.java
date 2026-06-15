package com.tyb.myblog.v2.content.domain;

import com.tyb.myblog.v2.content.domain.article.Article;
import com.tyb.myblog.v2.content.domain.article.ArticleSlug;
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import com.tyb.myblog.v2.content.domain.article.NewArticle;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 文章五态、字段组合、slug 和标签集合领域规则测试。
 */
class ArticleDomainTest {

    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 6, 15, 20, 0);
    private static final LocalDateTime CREATED_AT =
            NOW.minusDays(1);

    @Test
    void rejectsPublishedArticleWithoutRequiredContent() {
        assertThatThrownBy(() -> newArticle(
                null,
                null,
                null,
                ArticleStatus.PUBLISHED,
                null,
                NOW,
                List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void allowsIncompleteDraftAndNormalizesOptionalFields() {
        NewArticle article = NewArticle.create(
                "  ",
                null,
                null,
                null,
                null,
                null,
                "  ",
                null,
                1001L,
                "  ",
                ArticleStatus.DRAFT,
                null,
                null,
                null,
                List.of(),
                1001L,
                NOW);

        assertThat(article.titleZh()).isNull();
        assertThat(article.body()).isNull();
        assertThat(article.categoryId()).isNull();
        assertThat(article.slug()).isNull();
        assertThat(article.commentCount()).isZero();
    }

    @Test
    void normalizesSlugAndRejectsInvalidOrDuplicateTags() {
        assertThat(ArticleSlug.optional(" Spring-JWT ")
                .orElseThrow()
                .value())
                .isEqualTo("spring-jwt");
        assertThat(ArticleSlug.optional("  ")).isEmpty();
        assertThatThrownBy(() -> ArticleSlug.optional("spring--jwt"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> newArticle(
                "文章",
                "正文",
                10L,
                ArticleStatus.PUBLISHED,
                null,
                NOW,
                List.of(20L, 20L)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requiresPasswordHashAndScheduleTimeForMatchingStates() {
        assertThatThrownBy(() -> newArticle(
                "文章",
                "正文",
                10L,
                ArticleStatus.PASSWORD,
                null,
                NOW,
                List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> newArticle(
                "文章",
                "正文",
                10L,
                ArticleStatus.SCHEDULED,
                null,
                null,
                List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> newArticle(
                "文章",
                "正文",
                10L,
                ArticleStatus.SCHEDULED,
                null,
                NOW.minusSeconds(1),
                List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> newArticle(
                "文章",
                "正文",
                10L,
                ArticleStatus.PUBLISHED,
                "$2a$10$unexpected",
                NOW,
                List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createsValidPasswordAndScheduledArticles() {
        NewArticle password = newArticle(
                "密码文章",
                "正文",
                10L,
                ArticleStatus.PASSWORD,
                "$2a$10$hash",
                NOW,
                List.of(30L, 20L));
        NewArticle scheduled = newArticle(
                "定时文章",
                "正文",
                10L,
                ArticleStatus.SCHEDULED,
                null,
                NOW.plusMinutes(1),
                List.of());

        assertThat(password.tagIds()).containsExactly(20L, 30L);
        assertThat(password.accessPassword()).isEqualTo("$2a$10$hash");
        assertThat(scheduled.publishAt()).isEqualTo(NOW.plusMinutes(1));
    }

    @Test
    void reconstitutesDueScheduledArticleForPublisher() {
        Article article = Article.reconstitute(
                100L,
                "文章",
                null,
                null,
                null,
                null,
                null,
                "正文",
                10L,
                1001L,
                "article",
                ArticleStatus.SCHEDULED,
                null,
                NOW.minusMinutes(1),
                null,
                0,
                List.of(),
                CREATED_AT,
                1001L,
                CREATED_AT,
                1001L,
                false,
                null,
                null);

        assertThat(article.status()).isEqualTo(ArticleStatus.SCHEDULED);
        assertThat(article.publishAt()).isBefore(NOW);
    }

    @Test
    void rejectsUnknownDatabaseStatus() {
        assertThatThrownBy(() -> ArticleStatus.fromDatabase(99))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private NewArticle newArticle(
            String titleZh,
            String body,
            Long categoryId,
            ArticleStatus status,
            String accessPassword,
            LocalDateTime publishAt,
            List<Long> tagIds) {
        return NewArticle.create(
                titleZh,
                null,
                null,
                null,
                null,
                null,
                body,
                categoryId,
                1001L,
                " Article ",
                status,
                accessPassword,
                publishAt,
                null,
                tagIds,
                1001L,
                NOW);
    }
}
