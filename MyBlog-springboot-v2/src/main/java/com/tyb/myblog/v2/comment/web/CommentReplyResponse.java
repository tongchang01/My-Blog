package com.tyb.myblog.v2.comment.web;

import com.tyb.myblog.v2.comment.domain.CommentAuthor;
import com.tyb.myblog.v2.comment.domain.CommentReply;

import java.time.LocalDateTime;

/**
 * 前台评论回复响应。
 *
 * @param id        回复 ID
 * @param parentId  父评论 ID
 * @param author    回复作者
 * @param replyUser 被回复用户
 * @param content   回复内容
 * @param createdAt 创建时间
 */
public record CommentReplyResponse(
        int id,
        int parentId,
        Author author,
        Author replyUser,
        String content,
        LocalDateTime createdAt
) {
    /**
     * 从回复领域对象转换为响应。
     */
    static CommentReplyResponse from(CommentReply reply) {
        return new CommentReplyResponse(
                reply.id(),
                reply.parentId(),
                Author.from(reply.author()),
                Author.from(reply.replyUser()),
                reply.content(),
                reply.createdAt());
    }

    /**
     * 前台评论作者响应。
     *
     * @param id       用户 ID
     * @param nickname 昵称
     * @param avatar   头像
     * @param website  个人网站
     */
    public record Author(int id, String nickname, String avatar, String website) {
        /**
         * 从作者领域对象转换为响应。
         */
        static Author from(CommentAuthor author) {
            return new Author(author.id(), author.nickname(), author.avatar(), author.website());
        }
    }
}
