package com.tyb.myblog.v2.comment.application;

import com.tyb.myblog.v2.comment.domain.Comment;
import com.tyb.myblog.v2.comment.domain.CommentRepository;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class CommentThreadLock {

    private final CommentRepository repository;

    LockedComment active(long id) {
        Comment snapshot = repository.findActiveById(id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        if (snapshot.parentId() == null) {
            Comment root = repository.findActiveByIdForUpdate(id)
                    .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
            return new LockedComment(root, root);
        }
        Comment root = repository.findByIdForUpdate(snapshot.parentId())
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        Comment comment = repository.findActiveByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        return new LockedComment(comment, root);
    }

    LockedComment deleted(long id) {
        Comment snapshot = repository.findById(id)
                .filter(Comment::deleted)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        if (snapshot.parentId() == null) {
            Comment root = repository.findDeletedByIdForUpdate(id)
                    .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
            return new LockedComment(root, root);
        }
        Comment root = repository.findByIdForUpdate(snapshot.parentId())
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        Comment comment = repository.findDeletedByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        return new LockedComment(comment, root);
    }

    record LockedComment(Comment comment, Comment root) {
    }
}
