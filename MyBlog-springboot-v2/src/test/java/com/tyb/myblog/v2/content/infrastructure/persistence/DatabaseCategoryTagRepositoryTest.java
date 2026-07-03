package com.tyb.myblog.v2.content.infrastructure.persistence;

import com.tyb.myblog.v2.content.domain.category.Category;
import com.tyb.myblog.v2.content.domain.category.CategoryRepository;
import com.tyb.myblog.v2.content.domain.category.NewCategory;
import com.tyb.myblog.v2.content.domain.tag.NewTag;
import com.tyb.myblog.v2.content.domain.tag.Tag;
import com.tyb.myblog.v2.content.domain.tag.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class DatabaseCategoryTagRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TagRepository tagRepository;

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
    void readsOnlyActiveCategoriesInStableOrder() {
        insertCategory(101L, "Backend", "backend", 20, false);
        insertCategory(102L, "Frontend", "frontend", 10, false);
        insertCategory(103L, "Deleted", "deleted", 0, true);

        assertThat(categoryRepository.findAllActive())
                .extracting(Category::id)
                .containsExactly(102L, 101L);
    }

    @Test
    void readsOnlyActiveTagsInStableChineseNameOrder() {
        insertTag(201L, "Spring", "spring", false);
        insertTag(202L, "Java", "java", false);
        insertTag(203L, "Deleted", "deleted-tag", true);

        assertThat(tagRepository.findAllActive())
                .extracting(Tag::id)
                .containsExactly(202L, 201L);
    }

    @Test
    void readsPublicTaxonomyWithVisibleArticleCountsOnly() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 16, 12, 0);
        insertCategory(101L, "Backend", "backend", 10, false);
        insertCategory(102L, "Empty", "empty", 20, false);
        insertCategory(103L, "Deleted", "deleted", 30, true);
        insertTag(201L, "Java", "java", false);
        insertTag(202L, "Vue", "vue", true);
        insertTag(203L, "Empty", "empty-tag", false);
        insertArticle(301L, 101L, 2, "2026-06-15 10:00:00", false);
        insertArticle(302L, 101L, 4, "2026-06-15 11:00:00", false);
        insertArticle(303L, 101L, 1, "2026-06-15 12:00:00", false);
        insertArticle(304L, 101L, 2, "2026-06-17 10:00:00", false);
        insertArticle(305L, 101L, 2, "2026-06-15 13:00:00", true);
        insertArticle(306L, 103L, 2, "2026-06-15 14:00:00", false);
        linkTag(301L, 201L);
        linkTag(302L, 201L);
        linkTag(303L, 201L);
        linkTag(304L, 201L);
        linkTag(305L, 201L);
        linkTag(306L, 202L);

        assertThat(categoryRepository.findPublicWithArticleCount(now))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.category().id()).isEqualTo(101L);
                    assertThat(item.articleCount()).isEqualTo(2);
                });
        assertThat(tagRepository.findPublicWithArticleCount(now))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.tag().id()).isEqualTo(201L);
                    assertThat(item.articleCount()).isEqualTo(2);
                });
    }

    @Test
    void findsSlugIncludingDeletedRows() {
        insertCategory(101L, "Deleted", "old-category", 0, true);
        insertTag(201L, "Deleted", "old-tag", true);

        assertThat(categoryRepository.findBySlugIncludingDeleted(
                "old-category")).isPresent();
        assertThat(tagRepository.findBySlugIncludingDeleted(
                "old-tag")).isPresent();
    }

    @Test
    @Transactional
    void readsAndLocksOnlyActiveRowsInAscendingIdentityOrder() {
        insertCategory(101L, "Backend", "backend", 20, false);
        insertCategory(102L, "Deleted", "deleted", 0, true);
        insertCategory(103L, "Frontend", "frontend", 10, false);
        insertTag(201L, "Java", "java", false);
        insertTag(202L, "Deleted", "deleted-tag", true);

        assertThat(categoryRepository.findActiveById(101L)).isPresent();
        assertThat(categoryRepository.findActiveById(102L)).isEmpty();
        assertThat(categoryRepository.findActiveByIdForUpdate(101L))
                .isPresent();
        assertThat(categoryRepository.findActiveByIdsForUpdate(
                List.of(103L, 101L)))
                .extracting(Category::id)
                .containsExactly(101L, 103L);
        assertThat(tagRepository.findActiveById(201L)).isPresent();
        assertThat(tagRepository.findActiveById(202L)).isEmpty();
        assertThat(tagRepository.findActiveByIdForUpdate(201L))
                .isPresent();
        assertThat(tagRepository.findActiveByIdsForUpdate(
                List.of(202L, 201L)))
                .extracting(Tag::id)
                .containsExactly(201L);
    }

    @Test
    void insertsWithAssignedIdsAndExplicitAuditUsers() {
        Category category = categoryRepository.insert(
                NewCategory.create(
                        "Backend",
                        null,
                        "Backend",
                        "backend",
                        10,
                        1001L));
        Tag tag = tagRepository.insert(
                NewTag.create(
                        "Java",
                        null,
                        null,
                        "java",
                        1001L));

        assertThat(category.id()).isPositive();
        assertThat(category.createdBy()).isEqualTo(1001L);
        assertThat(category.updatedBy()).isEqualTo(1001L);
        assertThat(tag.id()).isPositive();
        assertThat(tag.createdBy()).isEqualTo(1001L);
        assertThat(tag.updatedBy()).isEqualTo(1001L);
        assertThat(jdbcTemplate.queryForMap("""
                SELECT created_by, updated_by
                FROM t_category
                WHERE id = ?
                """, category.id()))
                .containsEntry("CREATED_BY", 1001L)
                .containsEntry("UPDATED_BY", 1001L);
        assertThat(jdbcTemplate.queryForMap("""
                SELECT created_by, updated_by
                FROM t_tag
                WHERE id = ?
                """, tag.id()))
                .containsEntry("CREATED_BY", 1001L)
                .containsEntry("UPDATED_BY", 1001L);
    }

    @Test
    void fullyUpdatesActiveRowsAndAuditUsers() {
        insertCategory(101L, "Backend", "backend", 10, false);
        insertTag(201L, "Java", "java", false);
        LocalDateTime updatedAt =
                LocalDateTime.of(2026, 6, 15, 12, 0);

        Category category = categoryRepository.findActiveById(101L)
                .orElseThrow()
                .replace("Server", null, null, "server", 20);
        Tag tag = tagRepository.findActiveById(201L)
                .orElseThrow()
                .replace("Java 17", null, null, "java-17");

        assertThat(categoryRepository.update(
                category, updatedAt, 2001L)).isTrue();
        assertThat(tagRepository.update(
                tag, updatedAt, 2001L)).isTrue();
        assertThat(categoryRepository.findActiveById(101L))
                .get()
                .satisfies(updated -> {
                    assertThat(updated.name().zh()).isEqualTo("Server");
                    assertThat(updated.name().ja()).isNull();
                    assertThat(updated.slug().value()).isEqualTo("server");
                    assertThat(updated.sortOrder()).isEqualTo(20);
                    assertThat(updated.updatedAt()).isEqualTo(updatedAt);
                    assertThat(updated.updatedBy()).isEqualTo(2001L);
                });
        assertThat(tagRepository.findActiveById(201L))
                .get()
                .satisfies(updated -> {
                    assertThat(updated.name().zh()).isEqualTo("Java 17");
                    assertThat(updated.slug().value()).isEqualTo("java-17");
                    assertThat(updated.updatedBy()).isEqualTo(2001L);
                });
    }

    @Test
    void detectsOnlyActiveArticleReferencesAndSoftDeletesWithAudit() {
        insertCategory(101L, "Backend", "backend", 10, false);
        insertTag(201L, "Java", "java", false);
        insertArticle(301L, 101L, false);
        linkTag(301L, 201L);

        assertThat(categoryRepository
                .hasActiveArticleReference(101L)).isTrue();
        assertThat(tagRepository
                .hasActiveArticleReference(201L)).isTrue();

        jdbcTemplate.update("""
                UPDATE t_article
                SET deleted = 1,
                    deleted_at = '2026-06-15 11:00:00',
                    deleted_by = 1001
                WHERE id = ?
                """, 301L);
        assertThat(categoryRepository
                .hasActiveArticleReference(101L)).isFalse();
        assertThat(tagRepository
                .hasActiveArticleReference(201L)).isFalse();

        LocalDateTime deletedAt =
                LocalDateTime.of(2026, 6, 15, 12, 0);
        assertThat(categoryRepository.softDelete(
                101L, deletedAt, 2001L)).isTrue();
        assertThat(tagRepository.softDelete(
                201L, deletedAt, 2001L)).isTrue();
        assertThat(jdbcTemplate.queryForMap("""
                SELECT deleted, deleted_at, deleted_by,
                       updated_at, updated_by
                FROM t_category
                WHERE id = ?
                """, 101L))
                .containsEntry("DELETED", 1)
                .containsEntry("DELETED_BY", 2001L)
                .containsEntry("UPDATED_BY", 2001L);
        assertThat(jdbcTemplate.queryForMap("""
                SELECT deleted, deleted_at, deleted_by,
                       updated_at, updated_by
                FROM t_tag
                WHERE id = ?
                """, 201L))
                .containsEntry("DELETED", 1)
                .containsEntry("DELETED_BY", 2001L)
                .containsEntry("UPDATED_BY", 2001L);
    }

    private void insertCategory(
            long id,
            String nameZh,
            String slug,
            int sortOrder,
            boolean deleted) {
        jdbcTemplate.update("""
                INSERT INTO t_category (
                    id, name_zh, name_ja, name_en, slug, sort_order,
                    created_at, created_by, updated_at, updated_by,
                    deleted, deleted_at, deleted_by
                ) VALUES (?, ?, NULL, NULL, ?, ?,
                    '2026-06-15 10:00:00', 1001,
                    '2026-06-15 10:00:00', 1001,
                    ?, ?, ?)
                """,
                id,
                nameZh,
                slug,
                sortOrder,
                deleted ? 1 : 0,
                deleted ? "2026-06-15 11:00:00" : null,
                deleted ? 1001L : null);
    }

    private void insertTag(
            long id,
            String nameZh,
            String slug,
            boolean deleted) {
        jdbcTemplate.update("""
                INSERT INTO t_tag (
                    id, name_zh, name_ja, name_en, slug,
                    created_at, created_by, updated_at, updated_by,
                    deleted, deleted_at, deleted_by
                ) VALUES (?, ?, NULL, NULL, ?,
                    '2026-06-15 10:00:00', 1001,
                    '2026-06-15 10:00:00', 1001,
                    ?, ?, ?)
                """,
                id,
                nameZh,
                slug,
                deleted ? 1 : 0,
                deleted ? "2026-06-15 11:00:00" : null,
                deleted ? 1001L : null);
    }

    private void insertArticle(
            long id,
            long categoryId,
            boolean deleted) {
        insertArticle(
                id,
                categoryId,
                2,
                "2026-06-15 10:00:00",
                deleted);
    }

    private void insertArticle(
            long id,
            long categoryId,
            int status,
            String publishAt,
            boolean deleted) {
        jdbcTemplate.update("""
                INSERT INTO t_article (
                    id, title_zh, category_id, author_id,
                    slug, status, comment_count,
                    publish_at, created_at, created_by, updated_at, updated_by,
                    deleted, deleted_at, deleted_by
                ) VALUES (
                    ?, 'Article', ?, 1001,
                    ?, ?, 0,
                    ?,
                    '2026-06-15 10:00:00', 1001,
                    '2026-06-15 10:00:00', 1001,
                    ?, ?, ?
                )
                """,
                id,
                categoryId,
                "article-" + id,
                status,
                publishAt,
                deleted ? 1 : 0,
                deleted ? "2026-06-15 11:00:00" : null,
                deleted ? 1001L : null);
    }

    private void linkTag(long articleId, long tagId) {
        jdbcTemplate.update("""
                INSERT INTO t_article_tag (article_id, tag_id)
                VALUES (?, ?)
                """, articleId, tagId);
    }
}
