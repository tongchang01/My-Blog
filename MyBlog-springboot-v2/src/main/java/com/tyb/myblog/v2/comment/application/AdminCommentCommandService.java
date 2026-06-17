package com.tyb.myblog.v2.comment.application;

import com.tyb.myblog.v2.comment.domain.Comment;
import com.tyb.myblog.v2.comment.domain.CommentAuditStatus;
import com.tyb.myblog.v2.comment.domain.CommentRepository;
import com.tyb.myblog.v2.comment.domain.CommentTargetType;
import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.article.ArticleCommentCountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminCommentCommandService {

    private final CommentRepository repository;
    private final ArticleCommentCountService articleCommentCountService;
    private final CommentAuthorization authorization;
    private final Clock clock;

    @Transactional
    public void approve(AuthenticatedPrincipal principal, long id) {
        long operatorId = authorization.requireAdmin(principal);
        Comment comment = requireActive(id);
        updateStatus(comment, CommentAuditStatus.PASS, operatorId);
    }

    @Transactional
    public void hide(AuthenticatedPrincipal principal, long id) {
        long operatorId = authorization.requireAdmin(principal);
        Comment comment = requireActive(id);
        updateStatus(comment, CommentAuditStatus.HIDDEN, operatorId);
    }

    @Transactional
    public void delete(AuthenticatedPrincipal principal, long id) {
        long operatorId = authorization.requireAdmin(principal);
        Comment comment = requireActive(id);
        boolean updated = repository.softDelete(
                id,
                LocalDateTime.now(clock),
                operatorId);
        if (!updated) {
            throw new ApiException(ApiErrorCode.CONFLICT);
        }
        if (isPublicArticleComment(comment)) {
            articleCommentCountService.increment(comment.target().targetId(), -1);
        }
    }

    @Transactional
    public void restore(AuthenticatedPrincipal principal, long id) {
        long operatorId = authorization.requireAdmin(principal);
        Comment comment = repository.findDeletedByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        boolean updated = repository.restore(
                id,
                LocalDateTime.now(clock),
                operatorId);
        if (!updated) {
            throw new ApiException(ApiErrorCode.CONFLICT);
        }
        if (isPublicArticleComment(comment)) {
            articleCommentCountService.increment(comment.target().targetId(), 1);
        }
    }

    private void updateStatus(
            Comment comment,
            CommentAuditStatus next,
            long operatorId) {
        CommentAuditStatus previous = comment.auditStatus();
        if (previous == next) {
            return;
        }
        boolean updated = repository.updateAuditStatus(
                comment.id(),
                next,
                LocalDateTime.now(clock),
                operatorId);
        if (!updated) {
            throw new ApiException(ApiErrorCode.CONFLICT);
        }
        adjustArticleCount(comment, previous, next);
    }

    private void adjustArticleCount(
            Comment comment,
            CommentAuditStatus previous,
            CommentAuditStatus next) {
        if (comment.target().targetType() != CommentTargetType.ARTICLE) {
            return;
        }
        int delta = 0;
        if (!previous.publiclyVisible() && next.publiclyVisible()) {
            delta = 1;
        } else if (previous.publiclyVisible() && !next.publiclyVisible()) {
            delta = -1;
        }
        if (delta != 0) {
            articleCommentCountService.increment(comment.target().targetId(), delta);
        }
    }

    private Comment requireActive(long id) {
        return repository.findActiveById(id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
    }

    private static boolean isPublicArticleComment(Comment comment) {
        return comment.target().targetType() == CommentTargetType.ARTICLE
                && comment.auditStatus().publiclyVisible();
    }
}
