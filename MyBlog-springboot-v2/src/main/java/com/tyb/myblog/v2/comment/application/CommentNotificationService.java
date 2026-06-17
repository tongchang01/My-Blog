package com.tyb.myblog.v2.comment.application;

import com.tyb.myblog.v2.comment.domain.Comment;
import com.tyb.myblog.v2.comment.domain.CommentRepository;
import com.tyb.myblog.v2.common.mail.MailFailureLog;
import com.tyb.myblog.v2.common.mail.MailFailureLogRepository;
import com.tyb.myblog.v2.common.mail.MailSendCommand;
import com.tyb.myblog.v2.common.mail.MailSendResult;
import com.tyb.myblog.v2.common.mail.MailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentNotificationService {

    private static final String TEMPLATE = "comment_reply";

    private final CommentRepository repository;
    private final MailSender mailSender;
    private final MailFailureLogRepository failureLogRepository;
    private final Clock clock;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(CommentNotificationEvent event) {
        sendReplyNotification(event);
    }

    public void sendReplyNotification(CommentNotificationEvent event) {
        if (event.replyToCommentId() == null
                || !event.auditStatus().publiclyVisible()) {
            return;
        }
        repository.findActiveById(event.replyToCommentId())
                .filter(comment -> comment.auditStatus().publiclyVisible())
                .map(Comment::author)
                .ifPresent(author -> {
                    MailSendCommand command = command(event, author.email());
                    MailSendResult result = mailSender.send(command);
                    if (!result.success()) {
                        failureLogRepository.insertFailed(new MailFailureLog(
                                command.toEmail(),
                                command.template(),
                                command.subject(),
                                result.errorMessage(),
                                "{\"commentId\":\"" + event.commentId() + "\"}",
                                LocalDateTime.now(clock)));
                    }
                });
    }

    private static MailSendCommand command(
            CommentNotificationEvent event,
            String toEmail) {
        return new MailSendCommand(
                toEmail,
                TEMPLATE,
                "你的评论有新回复",
                event.authorNickname() + " 回复了你的评论。",
                Map.of("commentId", Long.toString(event.commentId())));
    }
}
