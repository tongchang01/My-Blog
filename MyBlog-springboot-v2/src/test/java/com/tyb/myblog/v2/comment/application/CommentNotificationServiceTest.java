package com.tyb.myblog.v2.comment.application;

import com.tyb.myblog.v2.comment.domain.Comment;
import com.tyb.myblog.v2.comment.domain.CommentAuditStatus;
import com.tyb.myblog.v2.comment.domain.CommentAuthor;
import com.tyb.myblog.v2.comment.domain.CommentContent;
import com.tyb.myblog.v2.comment.domain.CommentRepository;
import com.tyb.myblog.v2.comment.domain.CommentTarget;
import com.tyb.myblog.v2.common.mail.MailFailureLogRepository;
import com.tyb.myblog.v2.common.mail.MailSendResult;
import com.tyb.myblog.v2.common.mail.MailSender;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentNotificationServiceTest {

    private final CommentRepository repository = mock(CommentRepository.class);
    private final MailSender mailSender = mock(MailSender.class);
    private final MailFailureLogRepository failureLogRepository =
            mock(MailFailureLogRepository.class);
    private final CommentNotificationService service =
            new CommentNotificationService(
                    repository,
                    mailSender,
                    failureLogRepository,
                    Clock.fixed(
                            Instant.parse("2026-06-17T11:00:00Z"),
                            ZoneId.of("Asia/Tokyo")));

    @Test
    void passedReplyFailureWritesMailFailureLog() {
        when(repository.findActiveById(10L))
                .thenReturn(Optional.of(comment()));
        when(mailSender.send(any()))
                .thenReturn(MailSendResult.failed("HTTP 500"));

        service.sendReplyNotification(new CommentNotificationEvent(
                20L,
                10L,
                "TYB",
                "<p>reply</p>",
                CommentAuditStatus.PASS));

        verify(failureLogRepository).insertFailed(any());
    }

    @Test
    void pendingOrTopLevelCommentDoesNotSendMail() {
        service.sendReplyNotification(new CommentNotificationEvent(
                21L,
                10L,
                "TYB",
                "<p>reply</p>",
                CommentAuditStatus.PENDING));
        service.sendReplyNotification(new CommentNotificationEvent(
                22L,
                null,
                "TYB",
                "<p>top</p>",
                CommentAuditStatus.PASS));

        verify(mailSender, never()).send(any());
    }

    private static Comment comment() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 17, 20, 0);
        return Comment.reconstitute(
                10L,
                CommentTarget.article(100L),
                null,
                null,
                null,
                null,
                new CommentAuthor(
                        null,
                        "Reader",
                        "reader@example.com",
                        null,
                        "127.0.0.1",
                        "JUnit"),
                CommentContent.of("old", "<p>old</p>"),
                CommentAuditStatus.PASS,
                now,
                1L,
                now,
                1L,
                false,
                null,
                null);
    }
}
