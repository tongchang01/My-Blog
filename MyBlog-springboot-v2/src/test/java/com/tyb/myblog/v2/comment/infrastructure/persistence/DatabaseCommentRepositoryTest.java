package com.tyb.myblog.v2.comment.infrastructure.persistence;

import com.tyb.myblog.v2.comment.domain.Comment;
import com.tyb.myblog.v2.comment.domain.CommentAuditStatus;
import com.tyb.myblog.v2.comment.domain.CommentAuthor;
import com.tyb.myblog.v2.comment.domain.CommentContent;
import com.tyb.myblog.v2.comment.domain.CommentRepository;
import com.tyb.myblog.v2.comment.domain.CommentTarget;
import com.tyb.myblog.v2.comment.domain.NewComment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class DatabaseCommentRepositoryTest {

    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 6, 16, 20, 20);

    @Autowired
    private CommentRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetState() {
        jdbcTemplate.update("DELETE FROM t_comment");
    }

    @Test
    void insertsAssignedIdAndReadsActiveComment() {
        Comment inserted = repository.insert(newComment(null, null));

        Comment loaded = repository.findActiveById(inserted.id())
                .orElseThrow();
        Comment locked = repository.findActiveByIdForUpdate(inserted.id())
                .orElseThrow();

        assertThat(inserted.id()).isPositive();
        assertThat(loaded.target()).isEqualTo(CommentTarget.article(100L));
        assertThat(loaded.author().email()).isEqualTo("tyb@example.com");
        assertThat(loaded.auditStatus()).isEqualTo(CommentAuditStatus.PASS);
        assertThat(loaded.createdAt()).isNotNull();
        assertThat(locked.id()).isEqualTo(inserted.id());
    }

    @Test
    void insertsReplyAndKeepsReplyTargetSnapshot() {
        Comment root = repository.insert(newComment(null, null));
        Comment reply = repository.insert(newComment(root.id(), root.id()));

        Comment loaded = repository.findActiveById(reply.id())
                .orElseThrow();

        assertThat(loaded.parentId()).isEqualTo(root.id());
        assertThat(loaded.replyToCommentId()).isEqualTo(root.id());
        assertThat(loaded.replyToNickname()).isEqualTo("TYB");
        assertThat(repository.findById(root.id())).isPresent();
        assertThat(repository.findByIdForUpdate(root.id())).isPresent();
        assertThat(repository.countPublicRepliesForUpdate(root.id()))
                .isEqualTo(1);
    }

    @Test
    void updatesAuditStatusSoftDeletesAndRestores() {
        Comment inserted = repository.insert(newComment(null, null));

        assertThat(repository.updateAuditStatus(
                inserted.id(),
                CommentAuditStatus.HIDDEN,
                NOW.plusMinutes(1),
                1001L)).isTrue();
        assertThat(repository.findActiveById(inserted.id())
                .orElseThrow()
                .auditStatus())
                .isEqualTo(CommentAuditStatus.HIDDEN);

        assertThat(repository.softDelete(
                inserted.id(),
                NOW.plusMinutes(2),
                1001L)).isTrue();
        assertThat(repository.findActiveById(inserted.id())).isEmpty();
        assertThat(repository.findDeletedByIdForUpdate(inserted.id()))
                .isPresent();

        assertThat(repository.restore(
                inserted.id(),
                NOW.plusMinutes(3),
                1001L)).isTrue();
        assertThat(repository.findActiveById(inserted.id())).isPresent();
    }

    private NewComment newComment(Long parentId, Long replyToCommentId) {
        return NewComment.create(
                CommentTarget.article(100L),
                parentId,
                replyToCommentId,
                null,
                replyToCommentId == null ? null : "TYB",
                null,
                "TYB",
                "tyb@example.com",
                null,
                "127.0.0.1",
                "JUnit",
                "hello",
                "<p>hello</p>",
                CommentAuditStatus.PASS,
                NOW,
                null);
    }
}
