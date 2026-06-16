package com.tyb.myblog.v2.content.infrastructure.persistence;

import com.tyb.myblog.v2.content.domain.article.AdminArticleCriteria;
import com.tyb.myblog.v2.content.domain.article.AdminArticleQueryRepository;
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class DatabaseAdminArticleQueryRepositoryTest {

    @Autowired
    private AdminArticleQueryRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearTables() {
        jdbcTemplate.update("DELETE FROM t_article_tag");
        jdbcTemplate.update("DELETE FROM t_article");
        jdbcTemplate.update("DELETE FROM t_category");
        jdbcTemplate.update("DELETE FROM t_tag");
    }

    @Test
    void pagesActiveArticlesInUpdatedDescendingOrderAndFilters() {
        insertCategory(10L, "后端");
        insertCategory(11L, "前端");
        insertTag(20L, "Java");
        insertTag(21L, "Vue");
        insertArticle(100L, "Spring Boot", 2, 10L,
                "2026-06-15 10:00:00", false);
        insertArticle(101L, "Vue Admin", 1, 11L,
                "2026-06-15 12:00:00", false);
        insertArticle(102L, "Deleted", 2, 10L,
                "2026-06-15 13:00:00", true);
        linkTag(100L, 20L);
        linkTag(101L, 21L);

        assertThat(repository.findActivePage(query(null, null, null, null))
                .records())
                .extracting("id")
                .containsExactly(101L, 100L);
        assertThat(repository.findActivePage(query(
                ArticleStatus.PUBLISHED, 10L, 20L, "Spring")).records())
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.id()).isEqualTo(100L);
                    assertThat(item.categoryNameZh()).isEqualTo("后端");
                    assertThat(item.tagIds()).containsExactly(20L);
                });
    }

    @Test
    void readsDetailWithBodyCategoryTagsAndWithoutPasswordHash() {
        insertCategory(10L, "后端");
        insertTag(20L, "Java");
        insertTag(21L, "Spring");
        insertArticle(100L, "Password", 4, 10L,
                "2026-06-15 10:00:00", false);
        linkTag(100L, 21L);
        linkTag(100L, 20L);

        assertThat(repository.findActiveDetail(100L))
                .get()
                .satisfies(detail -> {
                    assertThat(detail.body()).isEqualTo("正文 100");
                    assertThat(detail.categoryNameZh()).isEqualTo("后端");
                    assertThat(detail.status())
                            .isEqualTo(ArticleStatus.PASSWORD);
                    assertThat(detail.tagIds()).containsExactly(20L, 21L);
                    assertThat(detail.coverAttachmentId()).isEqualTo(300L);
                });
    }

    private AdminArticleCriteria query(
            ArticleStatus status,
            Long categoryId,
            Long tagId,
            String keyword) {
        return new AdminArticleCriteria(
                1,
                20,
                status,
                categoryId,
                tagId,
                keyword,
                null,
                null,
                null,
                null);
    }

    private void insertCategory(long id, String name) {
        jdbcTemplate.update("""
                INSERT INTO t_category (
                    id, name_zh, slug, sort_order,
                    created_at, created_by, updated_at, updated_by, deleted
                ) VALUES (?, ?, ?, 0,
                    '2026-06-15 08:00:00', 1001,
                    '2026-06-15 08:00:00', 1001, 0)
                """, id, name, "category-" + id);
    }

    private void insertTag(long id, String name) {
        jdbcTemplate.update("""
                INSERT INTO t_tag (
                    id, name_zh, slug,
                    created_at, created_by, updated_at, updated_by, deleted
                ) VALUES (?, ?, ?,
                    '2026-06-15 08:00:00', 1001,
                    '2026-06-15 08:00:00', 1001, 0)
                """, id, name, "tag-" + id);
    }

    private void insertArticle(
            long id,
            String title,
            int status,
            long categoryId,
            String updatedAt,
            boolean deleted) {
        jdbcTemplate.update("""
                INSERT INTO t_article (
                    id, title_zh, title_ja, title_en,
                    summary_zh, body, category_id, author_id, slug,
                    status, access_password, publish_at,
                    cover_attachment_id, comment_count,
                    created_at, created_by, updated_at, updated_by,
                    deleted, deleted_at, deleted_by
                ) VALUES (
                    ?, ?, NULL, NULL,
                    ?, ?, ?, 1001, ?,
                    ?, ?, '2026-06-15 09:00:00',
                    300, 2,
                    '2026-06-15 08:00:00', 1001, ?, 1001,
                    ?, ?, ?
                )
                """,
                id,
                title,
                "摘要 " + id,
                "正文 " + id,
                categoryId,
                "article-" + id,
                status,
                status == 4 ? "secret-hash" : null,
                updatedAt,
                deleted ? 1 : 0,
                deleted ? LocalDateTime.of(2026, 6, 15, 14, 0) : null,
                deleted ? 1001L : null);
    }

    private void linkTag(long articleId, long tagId) {
        jdbcTemplate.update("""
                INSERT INTO t_article_tag (article_id, tag_id)
                VALUES (?, ?)
                """, articleId, tagId);
    }
}
