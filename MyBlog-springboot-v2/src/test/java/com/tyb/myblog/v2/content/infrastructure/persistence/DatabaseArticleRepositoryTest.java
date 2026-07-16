package com.tyb.myblog.v2.content.infrastructure.persistence;

import com.tyb.myblog.v2.content.domain.article.Article;
import com.tyb.myblog.v2.content.domain.article.ArticleRepository;
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import com.tyb.myblog.v2.content.domain.article.HomepageSlot;
import com.tyb.myblog.v2.content.domain.article.NewArticle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 文章聚合持久化、标签关联和状态解析真实数据库测试。
 */
@ActiveProfiles("test")
@SpringBootTest
class DatabaseArticleRepositoryTest {

    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 6, 15, 20, 0);

    @Autowired
    private ArticleRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetState() {
        jdbcTemplate.update("DELETE FROM t_article_tag");
        jdbcTemplate.update("DELETE FROM t_article");
    }

    @Test
    void insertsAssignedIdAndReadsStableTagOrder() {
        Article inserted = repository.insert(newArticle());
        repository.replaceTags(inserted.id(), List.of(30L, 20L));

        Article loaded = repository.findActiveById(inserted.id())
                .orElseThrow();

        assertThat(inserted.id()).isPositive();
        assertThat(loaded.commentCount()).isZero();
        assertThat(loaded.tagIds()).containsExactly(20L, 30L);
        assertThat(loaded.createdAt()).isNotNull();
        assertThat(loaded.createdBy()).isEqualTo(1001L);
        assertThat(loaded.updatedAt()).isNotNull();
        assertThat(loaded.updatedBy()).isEqualTo(1001L);
    }

    @Test
    void locksConfiguredHomepageSlot() {
        repository.lockHomepageSlot(HomepageSlot.PINNED);
    }

    @Test
    void fullyUpdatesActiveArticleWithoutChangingTags() {
        Article inserted = repository.insert(newArticle());
        repository.replaceTags(inserted.id(), List.of(20L));
        Article replacement = Article.reconstitute(
                inserted.id(),
                "更新文章",
                "更新記事",
                "Updated article",
                "更新摘要",
                null,
                null,
                "更新正文",
                11L,
                inserted.authorId(),
                "updated-article",
                ArticleStatus.PRIVATE,
                null,
                inserted.publishAt(),
                99L,
                inserted.commentCount(),
                List.of(20L),
                inserted.createdAt(),
                inserted.createdBy(),
                inserted.updatedAt(),
                inserted.updatedBy(),
                false,
                null,
                null);

        assertThat(repository.update(
                replacement,
                NOW.plusMinutes(1),
                1002L)).isTrue();

        Article loaded = repository.findActiveByIdForUpdate(inserted.id())
                .orElseThrow();
        assertThat(loaded.titleZh()).isEqualTo("更新文章");
        assertThat(loaded.status()).isEqualTo(ArticleStatus.PRIVATE);
        assertThat(loaded.coverAttachmentId()).isEqualTo(99L);
        assertThat(loaded.updatedBy()).isEqualTo(1002L);
        assertThat(loaded.tagIds()).containsExactly(20L);
    }

    @Test
    void readsDeletedArticleOnlyFromDeletedLockQuery() {
        Article inserted = repository.insert(newArticle());
        jdbcTemplate.update("""
                UPDATE t_article
                SET deleted = 1,
                    deleted_at = ?,
                    deleted_by = 1001,
                    updated_at = ?,
                    updated_by = 1001
                WHERE id = ?
                """,
                NOW,
                NOW,
                inserted.id());

        assertThat(repository.findActiveById(inserted.id())).isEmpty();
        Article deleted = repository
                .findDeletedByIdForUpdate(inserted.id())
                .orElseThrow();
        assertThat(deleted.deleted()).isTrue();
        assertThat(deleted.deletedAt()).isEqualTo(NOW);
        assertThat(deleted.deletedBy()).isEqualTo(1001L);
    }

    @Test
    void replacesAllTagRelationsAndAllowsEmptySet() {
        Article inserted = repository.insert(newArticle());
        repository.replaceTags(inserted.id(), List.of(30L, 20L));
        repository.replaceTags(inserted.id(), List.of(40L));

        assertThat(repository.findActiveById(inserted.id())
                .orElseThrow()
                .tagIds())
                .containsExactly(40L);

        repository.replaceTags(inserted.id(), List.of());
        assertThat(repository.findActiveById(inserted.id())
                .orElseThrow()
                .tagIds())
                .isEmpty();
    }

    @Test
    void rejectsUnknownPersistedStatus() {
        jdbcTemplate.update("""
                INSERT INTO t_article (
                    id, title_zh, body, category_id, author_id,
                    slug, status, publish_at, comment_count,
                    created_at, created_by, updated_at, updated_by,
                    deleted
                ) VALUES (
                    9001, '非法状态', '正文', 10, 1001,
                    'invalid-status', 99, ?, 0,
                    ?, 1001, ?, 1001,
                    0
                )
                """,
                NOW,
                NOW,
                NOW);

        assertThatThrownBy(() -> repository.findActiveById(9001L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("未知文章状态");
    }

    private NewArticle newArticle() {
        return NewArticle.create(
                "文章",
                null,
                "Article",
                "摘要",
                null,
                null,
                "正文",
                10L,
                1001L,
                "article",
                ArticleStatus.PUBLISHED,
                null,
                NOW,
                null,
                List.of(),
                1001L,
                NOW);
    }
}
