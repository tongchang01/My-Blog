package com.tyb.myblog.v2.content.infrastructure.persistence.repository;

import com.tyb.myblog.v2.content.domain.ContentSlugConflictException;
import com.tyb.myblog.v2.content.domain.category.Category;
import com.tyb.myblog.v2.content.domain.category.CategoryRepository;
import com.tyb.myblog.v2.content.domain.category.NewCategory;
import com.tyb.myblog.v2.content.infrastructure.persistence.entity.CategoryEntity;
import com.tyb.myblog.v2.content.infrastructure.persistence.mapper.CategoryMapper;
import com.tyb.myblog.v2.content.infrastructure.persistence.mapping.CategoryPersistenceMapping;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 基于 MyBatis 的分类仓储适配器。
 */
@Repository
@RequiredArgsConstructor
public class MyBatisCategoryRepository
        implements CategoryRepository {

    private final CategoryMapper mapper;
    private final CategoryPersistenceMapping mapping;

    @Override
    public List<Category> findAllActive() {
        return mapper.selectAllActive().stream()
                .map(mapping::toDomain)
                .toList();
    }

    @Override
    public Optional<Category> findActiveById(long id) {
        return Optional.ofNullable(mapper.selectActiveById(id))
                .map(mapping::toDomain);
    }

    @Override
    public Optional<Category> findActiveByIdForUpdate(long id) {
        return Optional.ofNullable(
                        mapper.selectActiveByIdForUpdate(id))
                .map(mapping::toDomain);
    }

    @Override
    public List<Category> findActiveByIdsForUpdate(
            List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return mapper.selectActiveByIdsForUpdate(ids).stream()
                .map(mapping::toDomain)
                .toList();
    }

    @Override
    public Optional<Category> findBySlugIncludingDeleted(
            String slug) {
        return Optional.ofNullable(
                        mapper.selectBySlugIncludingDeleted(slug))
                .map(mapping::toDomain);
    }

    @Override
    public Category insert(NewCategory category) {
        CategoryEntity entity = mapping.toEntity(category);
        try {
            if (mapper.insert(entity) != 1
                    || entity.getId() == null
                    || entity.getId() <= 0) {
                throw new IllegalStateException("分类新增失败");
            }
        } catch (DuplicateKeyException exception) {
            throw new ContentSlugConflictException();
        }
        return mapping.toDomain(entity);
    }

    @Override
    public boolean update(
            Category category,
            LocalDateTime updatedAt,
            long updatedBy) {
        try {
            return mapper.updateActive(
                    mapping.toEntity(category),
                    updatedAt,
                    updatedBy) == 1;
        } catch (DuplicateKeyException exception) {
            throw new ContentSlugConflictException();
        }
    }
}
