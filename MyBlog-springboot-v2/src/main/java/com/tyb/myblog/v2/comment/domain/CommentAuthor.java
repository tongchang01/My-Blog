package com.tyb.myblog.v2.comment.domain;

/**
 * 评论作者摘要。
 *
 * @param id       用户 ID
 * @param nickname 昵称
 * @param avatar   头像地址
 * @param website  个人网站
 */
public record CommentAuthor(int id, String nickname, String avatar, String website) {
}
