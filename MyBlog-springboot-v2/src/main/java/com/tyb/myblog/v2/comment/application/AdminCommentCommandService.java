package com.tyb.myblog.v2.comment.application;

import com.tyb.myblog.v2.comment.domain.Comment;
import com.tyb.myblog.v2.comment.domain.CommentAuditStatus;
import com.tyb.myblog.v2.comment.domain.CommentMarkdownRenderer;
import com.tyb.myblog.v2.comment.domain.CommentRepository;
import com.tyb.myblog.v2.comment.domain.CommentTargetType;
import com.tyb.myblog.v2.comment.domain.NewComment;
import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.article.ArticleCommentCountService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminCommentCommandService {

    private final CommentRepository repository;
    private final CommentThreadLock threadLock;
    private final ArticleCommentCountService articleCommentCountService;
    private final CommentAuthorization authorization;
    private final CommentMarkdownRenderer markdownRenderer;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Transactional
    public void approve(AuthenticatedPrincipal principal, long id) {
        long operatorId = authorization.requireAdmin(principal);
        CommentThreadLock.LockedComment locked = threadLock.active(id);
        updateStatus(locked, CommentAuditStatus.PASS, operatorId);
    }

    @Transactional
    public void hide(AuthenticatedPrincipal principal, long id) {
        long operatorId = authorization.requireAdmin(principal);
        CommentThreadLock.LockedComment locked = threadLock.active(id);
        updateStatus(locked, CommentAuditStatus.HIDDEN, operatorId);
    }

    @Transactional
    public void delete(AuthenticatedPrincipal principal, long id) {
        long operatorId = authorization.requireAdmin(principal);
        CommentThreadLock.LockedComment locked = threadLock.active(id);
        Comment comment = locked.comment();
        boolean updated = repository.softDelete(
                id,
                LocalDateTime.now(clock),
                operatorId);
        if (!updated) {
            throw new ApiException(ApiErrorCode.CONFLICT);
        }
        int visibleCount = visibleCount(locked);
        if (visibleCount != 0) {
            articleCommentCountService.increment(
                    comment.target().targetId(),
                    -visibleCount);
        }
    }

    @Transactional
    public void restore(AuthenticatedPrincipal principal, long id) {
        long operatorId = authorization.requireAdmin(principal);
        CommentThreadLock.LockedComment locked = threadLock.deleted(id);
        Comment comment = locked.comment();
        boolean updated = repository.restore(
                id,
                LocalDateTime.now(clock),
                operatorId);
        if (!updated) {
            throw new ApiException(ApiErrorCode.CONFLICT);
        }
        int visibleCount = restoredVisibleCount(locked);
        if (visibleCount != 0) {
            articleCommentCountService.increment(
                    comment.target().targetId(),
                    visibleCount);
        }
    }

    @Transactional
    public AdminCommentReplyResult reply(
            AuthenticatedPrincipal principal,
            long replyToCommentId,
            AdminCommentReplyCommand command) {
        long operatorId = authorization.requireAdmin(principal);
        CommentThreadLock.LockedComment locked = threadLock.active(
                replyToCommentId);
        Comment replyTo = locked.comment();
        if (!isPublic(locked.root())
                || !replyTo.auditStatus().publiclyVisible()) {
            throw new ApiException(ApiErrorCode.CONFLICT, "不能回复该评论");
        }
        long parentId = replyTo.parentId() == null
                ? replyTo.id()
                : replyTo.parentId();
        Comment inserted = repository.insert(NewComment.create(
                replyTo.target(),
                parentId,
                replyTo.id(),
                replyTo.author().userId(),
                replyTo.author().nickname(),
                operatorId,
                "站长",
                "admin@myblog.local",
                null,
                null,
                null,
                command.contentMd(),
                markdownRenderer.render(command.contentMd()),
                CommentAuditStatus.PASS,
                LocalDateTime.now(clock),
                operatorId));
        if (inserted.target().targetType() == CommentTargetType.ARTICLE) {
            articleCommentCountService.increment(inserted.target().targetId(), 1);
        }
        eventPublisher.publishEvent(new CommentNotificationEvent(
                inserted.id(),
                replyTo.id(),
                inserted.author().nickname(),
                inserted.content().safeHtml(),
                inserted.auditStatus()));
        return new AdminCommentReplyResult(
                inserted.id(),
                inserted.auditStatus());
    }

    private void updateStatus(
            CommentThreadLock.LockedComment locked,
            CommentAuditStatus next,
            long operatorId) {
        Comment comment = locked.comment();
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
        adjustArticleCount(locked, previous, next);
    }

    private void adjustArticleCount(
            CommentThreadLock.LockedComment locked,
            CommentAuditStatus previous,
            CommentAuditStatus next) {
        Comment comment = locked.comment();
        if (comment.target().targetType() != CommentTargetType.ARTICLE) {
            return;
        }
        if (comment.parentId() != null && !isPublic(locked.root())) {
            return;
        }
        int delta = 0;
        if (!previous.publiclyVisible() && next.publiclyVisible()) {
            delta = visibleThreadSize(comment);
        } else if (previous.publiclyVisible() && !next.publiclyVisible()) {
            delta = -visibleThreadSize(comment);
        }
        if (delta != 0) {
            articleCommentCountService.increment(comment.target().targetId(), delta);
        }
    }

    private int visibleCount(CommentThreadLock.LockedComment locked) {
        Comment comment = locked.comment();
        if (!isPublicArticleComment(comment) || !isPublic(locked.root())) {
            return 0;
        }
        return visibleThreadSize(comment);
    }

    private int restoredVisibleCount(
            CommentThreadLock.LockedComment locked) {
        Comment comment = locked.comment();
        if (comment.target().targetType() != CommentTargetType.ARTICLE
                || !comment.auditStatus().publiclyVisible()) {
            return 0;
        }
        if (comment.parentId() != null && !isPublic(locked.root())) {
            return 0;
        }
        return visibleThreadSize(comment);
    }

    private int visibleThreadSize(Comment comment) {
        return comment.parentId() == null
                ? 1 + repository.countPublicRepliesForUpdate(comment.id())
                : 1;
    }

    private static boolean isPublic(Comment comment) {
        return !comment.deleted()
                && comment.auditStatus().publiclyVisible();
    }

    private static boolean isPublicArticleComment(Comment comment) {
        return comment.target().targetType() == CommentTargetType.ARTICLE
                && comment.auditStatus().publiclyVisible();
    }
}
