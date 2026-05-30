package com.aurora.myblog.v2.modules.comment.api;

import com.aurora.myblog.v2.modules.comment.domain.CommentAuthor;
import com.aurora.myblog.v2.modules.comment.domain.CommentThread;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResponse(
        int id,
        int type,
        Integer topicId,
        Author author,
        String content,
        LocalDateTime createdAt,
        List<CommentReplyResponse> replies
) {
    static CommentResponse from(CommentThread comment) {
        return new CommentResponse(
                comment.id(),
                comment.type().code(),
                comment.topicId(),
                Author.from(comment.author()),
                comment.content(),
                comment.createdAt(),
                comment.replies().stream().map(CommentReplyResponse::from).toList());
    }

    public record Author(int id, String nickname, String avatar, String website) {
        static Author from(CommentAuthor author) {
            return new Author(author.id(), author.nickname(), author.avatar(), author.website());
        }
    }
}
