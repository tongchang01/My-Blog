package com.tyb.myblog.v2.content.domain.category;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 分类仓储端口。
 */
public interface CategoryRepository {

    List<Category> findAllActive();

    Optional<Category> findActiveById(long id);

    Optional<Category> findActiveByIdForUpdate(long id);

    List<Category> findActiveByIdsForUpdate(List<Long> ids);

    Optional<Category> findBySlugIncludingDeleted(String slug);

    Category insert(NewCategory category);

    boolean update(
            Category category,
            LocalDateTime updatedAt,
            long updatedBy);
}
