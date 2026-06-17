package com.tyb.myblog.v2.comment.application;

import com.tyb.myblog.v2.comment.domain.Comment;
import com.tyb.myblog.v2.comment.domain.CommentAuditStatus;
import com.tyb.myblog.v2.comment.domain.CommentAuthor;
import com.tyb.myblog.v2.comment.domain.CommentContent;
import com.tyb.myblog.v2.comment.domain.CommentRepository;
import com.tyb.myblog.v2.comment.domain.CommentTarget;
import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.article.ArticleCommentCountService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminCommentModerationServiceTest {

    private final CommentRepository repository = mock(CommentRepository.class);
    private final ArticleCommentCountService countService =
            mock(ArticleCommentCountService.class);
    private final AdminCommentCommandService service =
            new AdminCommentCommandService(
                    repository,
                    countService,
                    new CommentAuthorization(),
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
