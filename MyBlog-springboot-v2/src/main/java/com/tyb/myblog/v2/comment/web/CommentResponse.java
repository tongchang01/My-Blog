package com.tyb.myblog.v2.comment.web;

import com.tyb.myblog.v2.comment.domain.CommentAuthor;
import com.tyb.myblog.v2.comment.domain.CommentThread;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 前台一级评论响应。
 *
 * @param id        评论 ID
 * @param type      评论类型编码
 * @param topicId   主题 ID
 * @param author    评论作者
 * @param content   评论内容
 * @param createdAt 创建时间
 * @param replies   回复列表
 */
public record CommentResponse(
        int id,
        int type,
        Integer topicId,
        Author author,
        String content,
        LocalDateTime createdAt,
        List<CommentReplyResponse> replies
) {
    /**
     * 从评论线程领域对象转换为响应。
     */
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
