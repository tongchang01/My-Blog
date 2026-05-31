package com.tyb.myblog.v2.comment.web;

import com.tyb.myblog.v2.comment.domain.CommentAuthor;
import com.tyb.myblog.v2.comment.domain.CommentReply;

import java.time.LocalDateTime;

public record CommentReplyResponse(
        int id,
        int parentId,
        Author author,
        Author replyUser,
        String content,
        LocalDateTime createdAt
) {
    static CommentReplyResponse from(CommentReply reply) {
        return new CommentReplyResponse(
                reply.id(),
                reply.parentId(),
                Author.from(reply.author()),
                Author.from(reply.replyUser()),
                reply.content(),
                reply.createdAt());
    }

    public record Author(int id, String nickname, String avatar, String website) {
        static Author from(CommentAuthor author) {
            return new Author(author.id(), author.nickname(), author.avatar(), author.website());
        }
    }
}
