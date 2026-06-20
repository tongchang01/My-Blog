package com.tyb.myblog.v2.comment.application;

import com.tyb.myblog.v2.comment.domain.Comment;
import com.tyb.myblog.v2.comment.domain.CommentAuditStatus;
import com.tyb.myblog.v2.comment.domain.CommentContent;
import com.tyb.myblog.v2.comment.domain.CommentRepository;
import com.tyb.myblog.v2.comment.domain.CommentTarget;
import com.tyb.myblog.v2.comment.domain.NewComment;
import com.tyb.myblog.v2.content.application.article.ArticleCommentCountService;
import com.tyb.myblog.v2.content.application.article.ArticleCommentPolicy;
import com.tyb.myblog.v2.content.application.article.ArticleCommentPolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentCreateServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-17T00:00:00Z"),
            ZoneId.of("Asia/Tokyo"));

    private final CommentRepository repository = mock(CommentRepository.class);
    private final ArticleCommentPolicyService policyService =
            mock(ArticleCommentPolicyService.class);
    private final ArticleCommentCountService countService =
            mock(ArticleCommentCountService.class);
    private final CommentRateLimitService rateLimitService =
            mock(CommentRateLimitService.class);
    private final DuplicateCommentGuard duplicateGuard =
            mock(DuplicateCommentGuard.class);
    private final CommentAuditPolicy auditPolicy =
            mock(CommentAuditPolicy.class);
    private final ApplicationEventPublisher eventPublisher =
            mock(ApplicationEventPublisher.class);
    private final CommentCreateService service = new CommentCreateService(
            repository,
            policyService,
            countService,
            rateLimitService,
            duplicateGuard,
            auditPolicy,
            markdown -> "<p>" + markdown + "</p>",
            eventPublisher,
            CLOCK);

    @BeforeEach
    void defaultAuditPolicy() {
        when(auditPolicy.audit(any())).thenReturn(CommentAuditStatus.PASS);
    }

    @Test
    void createsPassingArticleCommentAndIncrementsCount() {
        when(policyService.requirePublicCommentable(100L))
                .thenReturn(new ArticleCommentPolicy(100L, 0));
        when(repository.insert(any()))
                .thenAnswer(invocation -> stored(invocation.getArgument(0), 200L));

        CommentCreateResult result = service.createArticleComment(command(100L, null, "hello"));

        assertThat(result.id()).isEqualTo(200L);
        assertThat(result.auditStatus()).isEqualTo(CommentAuditStatus.PASS);
        verify(countService).increment(100L, 1);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void blacklistedCommentBecomesPendingAndDoesNotIncrementCount() {
        when(policyService.requirePublicCommentable(100L))
                .thenReturn(new ArticleCommentPolicy(100L, 0));
        when(auditPolicy.audit("spam"))
                .thenReturn(CommentAuditStatus.PENDING);
        when(repository.insert(any()))
                .thenAnswer(invocation -> stored(invocation.getArgument(0), 201L));

        CommentCreateResult result = service.createArticleComment(command(100L, null, "spam"));

        assertThat(result.auditStatus()).isEqualTo(CommentAuditStatus.PENDING);
        verify(countService, never()).increment(100L, 1);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void guestbookCommentDoesNotTouchArticleCount() {
        when(repository.insert(any()))
                .thenAnswer(invocation -> stored(invocation.getArgument(0), 300L));

        CommentCreateResult result = service.createGuestbookComment(
                command(0L, null, "hello"));

        assertThat(result.id()).isEqualTo(300L);
        verify(countService, never()).increment(any(Long.class), any(Integer.class));
    }

    @Test
    void rejectsCrossTargetReply() {
        Comment existing = Comment.reconstitute(
                10L,
                CommentTarget.guestbook(),
                null,
                null,
                null,
                null,
                com.tyb.myblog.v2.comment.domain.CommentAuthor.guest(
                        "A", "a@example.com", null, null, null),
                CommentContent.of("old", "<p>old</p>"),
                CommentAuditStatus.PASS,
                LocalDateTime.now(CLOCK),
                null,
                LocalDateTime.now(CLOCK),
                null,
                false,
                null,
                null);
        when(policyService.requirePublicCommentable(100L))
                .thenReturn(new ArticleCommentPolicy(100L, 0));
        when(repository.findActiveById(10L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.createArticleComment(command(100L, 10L, "reply")))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void publishesNotificationEventForPassedReply() {
        Comment existing = Comment.reconstitute(
                10L,
                CommentTarget.article(100L),
                null,
                null,
                null,
                null,
                com.tyb.myblog.v2.comment.domain.CommentAuthor.guest(
                        "A", "a@example.com", null, null, null),
                CommentContent.of("old", "<p>old</p>"),
                CommentAuditStatus.PASS,
                LocalDateTime.now(CLOCK),
                null,
                LocalDateTime.now(CLOCK),
                null,
                false,
                null,
                null);
        when(policyService.requirePublicCommentable(100L))
                .thenReturn(new ArticleCommentPolicy(100L, 0));
        when(repository.findActiveById(10L)).thenReturn(Optional.of(existing));
        when(repository.insert(any()))
                .thenAnswer(invocation -> stored(invocation.getArgument(0), 202L));

        service.createArticleComment(command(100L, 10L, "reply"));

        verify(eventPublisher).publishEvent(any(CommentNotificationEvent.class));
    }

    private static CommentCreateCommand command(
            long targetId,
            Long replyTo,
            String content) {
        return new CommentCreateCommand(
                targetId,
                "TYB",
                "tyb@example.com",
                null,
                content,
                replyTo,
                "127.0.0.1",
                "JUnit");
    }

    private static Comment stored(NewComment comment, long id) {
        return Comment.reconstitute(
                id,
                comment.target(),
                comment.parentId(),
                comment.replyToCommentId(),
                comment.replyToUserId(),
                comment.replyToNickname(),
                comment.author(),
                comment.content(),
                comment.auditStatus(),
                comment.createdAt(),
                comment.createdBy(),
                comment.createdAt(),
                comment.createdBy(),
                false,
                null,
                null);
    }
}
