package com.tyb.myblog.v2.comment.application;

import com.tyb.myblog.v2.comment.domain.Comment;
import com.tyb.myblog.v2.comment.domain.CommentAuditStatus;
import com.tyb.myblog.v2.comment.domain.CommentAuthor;
import com.tyb.myblog.v2.comment.domain.CommentContent;
import com.tyb.myblog.v2.comment.domain.CommentMarkdownRenderer;
import com.tyb.myblog.v2.comment.domain.CommentRepository;
import com.tyb.myblog.v2.comment.domain.CommentTarget;
import com.tyb.myblog.v2.comment.domain.NewComment;
import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.article.ArticleCommentCountService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminCommentModerationServiceTest {

    private final CommentRepository repository = mock(CommentRepository.class);
    private final ArticleCommentCountService countService =
            mock(ArticleCommentCountService.class);
    private final CommentMarkdownRenderer markdownRenderer =
            mock(CommentMarkdownRenderer.class);
    private final ApplicationEventPublisher eventPublisher =
            mock(ApplicationEventPublisher.class);
    private final AdminCommentCommandService service =
            new AdminCommentCommandService(
                    repository,
                    countService,
                    new CommentAuthorization(),
                    markdownRenderer,
                    eventPublisher,
                    Clock.fixed(
                            Instant.parse("2026-06-17T10:50:00Z"),
                            ZoneId.of("Asia/Tokyo")));

    @Test
    void approvesPendingArticleCommentAndIncrementsCount() {
        when(repository.findActiveById(10L))
                .thenReturn(Optional.of(comment(CommentAuditStatus.PENDING, false)));
        when(repository.updateAuditStatus(
                10L,
                CommentAuditStatus.PASS,
                LocalDateTime.of(2026, 6, 17, 19, 50),
                1001L))
                .thenReturn(true);

        service.approve(admin(), 10L);

        verify(countService).increment(100L, 1);
    }

    @Test
    void hidesPassedArticleCommentAndDecrementsCount() {
        when(repository.findActiveById(10L))
                .thenReturn(Optional.of(comment(CommentAuditStatus.PASS, false)));
        when(repository.updateAuditStatus(
                10L,
                CommentAuditStatus.HIDDEN,
                LocalDateTime.of(2026, 6, 17, 19, 50),
                1001L))
                .thenReturn(true);

        service.hide(admin(), 10L);

        verify(countService).increment(100L, -1);
    }

    @Test
    void softDeletesPassedArticleCommentAndDecrementsCount() {
        when(repository.findActiveById(10L))
                .thenReturn(Optional.of(comment(CommentAuditStatus.PASS, false)));
        when(repository.softDelete(
                10L,
                LocalDateTime.of(2026, 6, 17, 19, 50),
                1001L))
                .thenReturn(true);

        service.delete(admin(), 10L);

        verify(countService).increment(100L, -1);
    }

    @Test
    void restoresHiddenCommentWithoutChangingCount() {
        when(repository.findDeletedByIdForUpdate(10L))
                .thenReturn(Optional.of(comment(CommentAuditStatus.HIDDEN, true)));
        when(repository.restore(
                10L,
                LocalDateTime.of(2026, 6, 17, 19, 50),
                1001L))
                .thenReturn(true);

        service.restore(admin(), 10L);

        verify(countService, never()).increment(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void demoCanNotModerateComments() {
        assertThatThrownBy(() -> service.approve(demo(), 10L))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void adminCanReplyToPassedArticleComment() {
        when(repository.findActiveById(10L))
                .thenReturn(Optional.of(comment(CommentAuditStatus.PASS, false)));
        when(markdownRenderer.render("谢谢反馈"))
                .thenReturn("<p>谢谢反馈</p>");
        when(repository.insert(any(NewComment.class)))
                .thenAnswer(invocation -> {
                    NewComment created = invocation.getArgument(0);
                    return Comment.reconstitute(
                            20L,
                            created.target(),
                            created.parentId(),
                            created.replyToCommentId(),
                            created.replyToUserId(),
                            created.replyToNickname(),
                            created.author(),
                            created.content(),
                            created.auditStatus(),
                            created.createdAt(),
                            created.createdBy(),
                            created.createdAt(),
                            created.createdBy(),
                            false,
                            null,
                            null);
                });

        AdminCommentReplyResult result = service.reply(
                admin(),
                10L,
                new AdminCommentReplyCommand("谢谢反馈"));

        assertThat(result.id()).isEqualTo(20L);
        assertThat(result.auditStatus()).isEqualTo(CommentAuditStatus.PASS);
        org.mockito.ArgumentCaptor<NewComment> captor =
                org.mockito.ArgumentCaptor.forClass(NewComment.class);
        verify(repository).insert(captor.capture());
        NewComment inserted = captor.getValue();
        assertThat(inserted.target()).isEqualTo(CommentTarget.article(100L));
        assertThat(inserted.parentId()).isEqualTo(10L);
        assertThat(inserted.replyToCommentId()).isEqualTo(10L);
        assertThat(inserted.replyToNickname()).isEqualTo("TYB");
        assertThat(inserted.author().userId()).isEqualTo(1001L);
        assertThat(inserted.author().nickname()).isEqualTo("站长");
        assertThat(inserted.content().markdown()).isEqualTo("谢谢反馈");
        assertThat(inserted.content().safeHtml()).isEqualTo("<p>谢谢反馈</p>");
        assertThat(inserted.auditStatus()).isEqualTo(CommentAuditStatus.PASS);
        assertThat(inserted.createdBy()).isEqualTo(1001L);
        verify(countService).increment(100L, 1);
        verify(eventPublisher).publishEvent(any(CommentNotificationEvent.class));
    }

    @Test
    void rejectsReplyToHiddenComment() {
        when(repository.findActiveById(10L))
                .thenReturn(Optional.of(comment(CommentAuditStatus.HIDDEN, false)));

        assertThatThrownBy(() -> service.reply(
                admin(),
                10L,
                new AdminCommentReplyCommand("谢谢反馈")))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).code())
                .isEqualTo(ApiErrorCode.CONFLICT);
        verify(repository, never()).insert(any(NewComment.class));
    }

    private static Comment comment(
            CommentAuditStatus status,
            boolean deleted) {
        LocalDateTime now = LocalDateTime.of(2026, 6, 17, 19, 30);
        return Comment.reconstitute(
                10L,
                CommentTarget.article(100L),
                null,
                null,
                null,
                null,
                new CommentAuthor(
                        null,
                        "TYB",
                        "tyb@example.com",
                        "https://example.com",
                        "127.0.0.1",
                        "JUnit"),
                CommentContent.of("hello", "<p>hello</p>"),
                status,
                now,
                1L,
                now,
                1L,
                deleted,
                deleted ? now : null,
                deleted ? 1001L : null);
    }

    private static AuthenticatedPrincipal admin() {
        return new AuthenticatedPrincipal("1001", "admin", List.of("ADMIN"));
    }

    private static AuthenticatedPrincipal demo() {
        return new AuthenticatedPrincipal("1002", "demo", List.of("DEMO"));
    }
}
