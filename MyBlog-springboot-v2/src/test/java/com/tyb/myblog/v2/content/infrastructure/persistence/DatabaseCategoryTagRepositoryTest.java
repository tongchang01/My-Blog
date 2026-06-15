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
    }

    @Test
    void insertsWithAssignedIdsAndExplicitAuditUsers() {
        Category category = categoryRepository.insert(
                NewCategory.create(
                        "后端",
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
}
