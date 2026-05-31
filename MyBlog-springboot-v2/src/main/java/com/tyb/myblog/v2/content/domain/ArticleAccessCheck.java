package com.tyb.myblog.v2.content.domain;

/**
 * 文章访问状态检查结果。
 *
 * @param id      文章 ID
 * @param status  旧库文章状态，{@code 1} 表示公开发布，{@code 2} 表示密码保护
 * @param deleted 是否已软删除，对应旧库 {@code t_article.is_delete}
 * @param password 受保护文章访问密码
 */
public record ArticleAccessCheck(int id, int status, boolean deleted, String password) {

    /**
     * 是否为可直接公开访问的文章。
     */
    public boolean publicArticle() {
        return !deleted && status == 1;
    }

    /**
     * 是否为需要密码访问的文章。
     */
    public boolean protectedArticle() {
        return !deleted && status == 2;
    }
}
