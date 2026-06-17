package com.tyb.myblog.v2.comment.application;

import com.tyb.myblog.v2.comment.domain.Comment;
import com.tyb.myblog.v2.comment.domain.CommentAuditStatus;
import com.tyb.myblog.v2.comment.domain.CommentMarkdownRenderer;
import com.tyb.myblog.v2.comment.domain.CommentRepository;
import com.tyb.myblog.v2.comment.domain.CommentTarget;
import com.tyb.myblog.v2.comment.domain.NewComment;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.article.ArticleCommentCountService;
import com.tyb.myblog.v2.content.application.article.ArticleCommentPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CommentCreateService {

    private final CommentRepository repository;
    private final ArticleCommentPolicyService articlePolicyService;
    private final ArticleCommentCountService articleCountService;
    private final CommentRateLimitService rateLimitService;
    private final DuplicateCommentGuard duplicateGuard;
    private final CommentMarkdownRenderer markdownRenderer;
    private final Clock clock;

    @Transactional
    public CommentCreateResult createArticleComment(
            CommentCreateCommand command) {
        articlePolicyService.requirePublicCommentable(command.targetId());
        return create(CommentTarget.article(command.targetId()), command, true);
    }

    @Transactional
    public CommentCreateResult createGuestbookComment(
            CommentCreateCommand command) {
        return create(CommentTarget.guestbook(), command, false);
    }

    private CommentCreateResult create(
            CommentTarget target,
            CommentCreateCommand command,
            boolean articleCounted) {
        rateLimitService.checkAndRecord(command.clientIp());
        duplicateGuard.checkAndRecord(
                command.clientIp(),
                target,
                command.contentMd());
        ReplySnapshot reply = resolveReply(target, command.replyToCommentId());
        CommentAuditStatus status = audit(command.contentMd());
        Comment inserted = repository.insert(NewComment.create(
                target,
                reply.parentId(),
                command.replyToCommentId(),
                reply.replyToUserId(),
                reply.replyToNickname(),
                null,
                command.nickname(),
                command.email(),
                command.site(),
                command.clientIp(),
                command.userAgent(),
                command.contentMd(),
                markdownRenderer.render(command.contentMd()),
                status,
                LocalDateTime.now(clock),
                null));
        if (articleCounted && status.publiclyVisible()) {
            articleCountService.increment(target.targetId(), 1);
        }
        return new CommentCreateResult(inserted.id(), inserted.auditStatus());
    }

    private ReplySnapshot resolveReply(
            CommentTarget target,
            Long replyToCommentId) {
        if (replyToCommentId == null) {
            return ReplySnapshot.none();
        }
        Comment replyTo = repository.findActiveById(replyToCommentId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        if (!replyTo.target().equals(target)
                || !replyTo.auditStatus().publiclyVisible()) {
            throw new ApiException(ApiErrorCode.CONFLICT, "不能回复该评论");
        }
        long parentId = replyTo.parentId() == null
                ? replyTo.id()
                : replyTo.parentId();
        return new ReplySnapshot(
                parentId,
                replyTo.author().userId(),
                replyTo.author().nickname());
    }

    private CommentAuditStatus audit(String contentMd) {
        if (contentMd != null
                && contentMd.toLowerCase(java.util.Locale.ROOT).contains("spam")) {
            return CommentAuditStatus.PENDING;
        }
        return CommentAuditStatus.PASS;
    }

    private record ReplySnapshot(
            Long parentId,
            Long replyToUserId,
            String replyToNickname) {

        static ReplySnapshot none() {
            return new ReplySnapshot(null, null, null);
        }
    }
}
