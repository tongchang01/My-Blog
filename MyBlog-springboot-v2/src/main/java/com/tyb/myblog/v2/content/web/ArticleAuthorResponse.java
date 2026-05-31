package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.domain.AuthorSummary;

/**
 * 文章作者响应。
 *
 * @param id       用户资料 ID
 * @param nickname 昵称
 * @param avatar   头像地址
 */
public record ArticleAuthorResponse(int id, String nickname, String avatar) {

    static ArticleAuthorResponse from(AuthorSummary author) {
        return new ArticleAuthorResponse(author.id(), author.nickname(), author.avatar());
    }
}
