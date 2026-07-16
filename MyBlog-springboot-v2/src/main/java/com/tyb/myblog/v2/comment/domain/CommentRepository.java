package com.tyb.myblog.v2.comment.domain;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CommentRepository {

    Comment insert(NewComment comment);

    Optional<Comment> findActiveById(long id);

    Optional<Comment> findActiveByIdForUpdate(long id);

    Optional<Comment> findDeletedByIdForUpdate(long id);

    boolean updateAuditStatus(
            long id,
            CommentAuditStatus status,
            LocalDateTime updatedAt,
            long updatedBy);

    boolean softDelete(
            long id,
            LocalDateTime deletedAt,
            long deletedBy);

    boolean restore(
            long id,
            LocalDateTime updatedAt,
            long updatedBy);
}
