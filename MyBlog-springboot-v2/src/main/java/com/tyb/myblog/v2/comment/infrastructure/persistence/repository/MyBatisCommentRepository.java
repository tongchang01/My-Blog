package com.tyb.myblog.v2.comment.infrastructure.persistence.repository;

import com.tyb.myblog.v2.comment.domain.Comment;
import com.tyb.myblog.v2.comment.domain.CommentAuditStatus;
import com.tyb.myblog.v2.comment.domain.CommentRepository;
import com.tyb.myblog.v2.comment.domain.NewComment;
import com.tyb.myblog.v2.comment.infrastructure.persistence.entity.CommentEntity;
import com.tyb.myblog.v2.comment.infrastructure.persistence.mapper.CommentMapper;
import com.tyb.myblog.v2.comment.infrastructure.persistence.mapping.CommentPersistenceMapping;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MyBatisCommentRepository implements CommentRepository {

    private final CommentMapper mapper;
    private final CommentPersistenceMapping mapping;

    @Override
    public Comment insert(NewComment comment) {
        CommentEntity entity = mapping.toEntity(comment);
        if (mapper.insert(entity) != 1
                || entity.getId() == null
                || entity.getId() <= 0) {
            throw new IllegalStateException("评论写入失败");
        }
        return mapping.toDomain(entity);
    }

    @Override
    public Optional<Comment> findActiveById(long id) {
        return toDomain(mapper.selectActiveById(id));
    }

    @Override
    public Optional<Comment> findActiveByIdForUpdate(long id) {
        return toDomain(mapper.selectActiveByIdForUpdate(id));
    }

    @Override
    public Optional<Comment> findDeletedByIdForUpdate(long id) {
        return toDomain(mapper.selectDeletedByIdForUpdate(id));
    }

    @Override
    public boolean updateAuditStatus(
            long id,
            CommentAuditStatus status,
            LocalDateTime updatedAt,
            long updatedBy) {
        return mapper.updateAuditStatus(
                id,
                status,
                updatedAt,
                updatedBy) == 1;
    }

    @Override
    public boolean softDelete(
            long id,
            LocalDateTime deletedAt,
            long deletedBy) {
        return mapper.softDelete(id, deletedAt, deletedBy) == 1;
    }

    @Override
    public boolean restore(
            long id,
            LocalDateTime updatedAt,
            long updatedBy) {
        return mapper.restore(id, updatedAt, updatedBy) == 1;
    }

    private Optional<Comment> toDomain(CommentEntity entity) {
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(mapping.toDomain(entity));
    }
}
