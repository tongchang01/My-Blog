package com.tyb.myblog.v2.comment.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommentDomainTest {

    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 6, 16, 20, 20);

    @Test
    void createsTopLevelCommentAndNormalizesAuthorContent() {
        NewComment comment = NewComment.create(
                CommentTarget.article(100L),
                null,
                null,
                null,
                null,
                null,
                "  TYB  ",
                " TYB@example.COM ",
                " https://example.com ",
                "127.0.0.1",
                "Mozilla",
                "  **hello**  ",
                "<p><strong>hello</strong></p>",
                CommentAuditStatus.PASS,
                NOW,
                null);

        assertThat(comment.target()).isEqualTo(CommentTarget.article(100L));
        assertThat(comment.parentId()).isNull();
        assertThat(comment.author().nickname()).isEqualTo("TYB");
        assertThat(comment.author().email()).isEqualTo("tyb@example.com");
        assertThat(comment.author().site()).isEqualTo("https://example.com");
        assertThat(comment.content().markdown()).isEqualTo("**hello**");
        assertThat(comment.createdBy()).isNull();
    }

    @Test
    void validatesTargetReplyAndContentBoundaries() {
        assertThat(CommentTarget.guestbook().targetId()).isZero();
        assertThatThrownBy(() -> CommentTarget.of(CommentTargetType.GUESTBOOK, 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> NewComment.create(
                CommentTarget.article(100L),
                10L,
                null,
                null,
                null,
                null,
                "TYB",
                "tyb@example.com",
                null,
                null,
                null,
                "hello",
                "<p>hello</p>",
                CommentAuditStatus.PASS,
                NOW,
                null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> NewComment.create(
                CommentTarget.article(100L),
                null,
                null,
                null,
                null,
                null,
                "TYB",
                "bad-email",
                "ftp://example.com",
                null,
                null,
                "hello",
                "<p>hello</p>",
                CommentAuditStatus.PASS,
                NOW,
                null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mapsDatabaseEnumsAndValidatesDeletedAudit() {
        assertThat(CommentTargetType.fromDatabase(1))
                .isEqualTo(CommentTargetType.ARTICLE);
        assertThat(CommentAuditStatus.fromDatabase(3))
                .isEqualTo(CommentAuditStatus.HIDDEN);
        assertThatThrownBy(() -> CommentAuditStatus.fromDatabase(99))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Comment.reconstitute(
                1L,
                CommentTarget.article(100L),
                null,
                null,
                null,
                null,
                CommentAuthor.guest(
                        "TYB",
                        "tyb@example.com",
                        null,
                        null,
                        null),
                CommentContent.of("hello", "<p>hello</p>"),
                CommentAuditStatus.PASS,
                NOW,
                null,
                NOW,
                null,
                true,
                null,
                null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
