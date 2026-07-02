package com.tyb.myblog.v2.content.domain.category;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 分类仓储端口。
 */
public interface CategoryRepository {

    List<Category> findAllActive();

    List<PublicCategoryWithArticleCount> findPublicWithArticleCount(
            LocalDateTime now);

    Optional<Category> findActiveById(long id);

    Optional<Category> findActiveByIdForUpdate(long id);

    List<Category> findActiveByIdsForUpdate(List<Long> ids);

    Optional<Category> findBySlugIncludingDeleted(String slug);

    Category insert(NewCategory category);

    boolean update(
            Category category,
            LocalDateTime updatedAt,
            long updatedBy);

    boolean updateSortOrder(
            long id,
            int sortOrder,
            LocalDateTime updatedAt,
            long updatedBy);

    boolean hasActiveArticleReference(long categoryId);

    boolean softDelete(
            long id,
            LocalDateTime deletedAt,
            long deletedBy);

    record PublicCategoryWithArticleCount(
            Category category,
            long articleCount) {
    }
}
