package com.tyb.myblog.v2.content.infrastructure.persistence.repository;

import com.tyb.myblog.v2.content.domain.ContentSlugConflictException;
import com.tyb.myblog.v2.content.domain.tag.NewTag;
import com.tyb.myblog.v2.content.domain.tag.Tag;
import com.tyb.myblog.v2.content.domain.tag.TagRepository;
import com.tyb.myblog.v2.content.infrastructure.persistence.entity.TagEntity;
import com.tyb.myblog.v2.content.infrastructure.persistence.mapper.TagMapper;
import com.tyb.myblog.v2.content.infrastructure.persistence.mapping.TagPersistenceMapping;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 基于 MyBatis 的标签仓储适配器。
 */
@Repository
@RequiredArgsConstructor
public class MyBatisTagRepository implements TagRepository {

    private final TagMapper mapper;
    private final TagPersistenceMapping mapping;

    @Override
    public List<Tag> findAllActive() {
        return mapper.selectAllActive().stream()
                .map(mapping::toDomain)
                .toList();
    }

    @Override
    public Optional<Tag> findActiveById(long id) {
        return Optional.ofNullable(mapper.selectActiveById(id))
                .map(mapping::toDomain);
    }

    @Override
    public Optional<Tag> findActiveByIdForUpdate(long id) {
        return Optional.ofNullable(
                        mapper.selectActiveByIdForUpdate(id))
                .map(mapping::toDomain);
    }

    @Override
    public Optional<Tag> findBySlugIncludingDeleted(String slug) {
        return Optional.ofNullable(
                        mapper.selectBySlugIncludingDeleted(slug))
                .map(mapping::toDomain);
    }

    @Override
    public Tag insert(NewTag tag) {
        TagEntity entity = mapping.toEntity(tag);
        try {
            if (mapper.insert(entity) != 1
                    || entity.getId() == null
                    || entity.getId() <= 0) {
                throw new IllegalStateException("标签新增失败");
            }
        } catch (DuplicateKeyException exception) {
            throw new ContentSlugConflictException();
        }
        return mapping.toDomain(entity);
    }

    @Override
    public boolean update(
            Tag tag,
            LocalDateTime updatedAt,
            long updatedBy) {
        try {
            return mapper.updateActive(
                    mapping.toEntity(tag),
                    updatedAt,
                    updatedBy) == 1;
        } catch (DuplicateKeyException exception) {
            throw new ContentSlugConflictException();
        }
    }

    @Override
    public boolean hasActiveArticleReference(long tagId) {
        return mapper.existsActiveArticleReference(tagId);
    }

    @Override
    public boolean softDelete(
            long id,
            LocalDateTime deletedAt,
            long deletedBy) {
        return mapper.softDelete(id, deletedAt, deletedBy) == 1;
    }
}
