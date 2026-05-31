package com.tyb.myblog.v2.content.domain;

/**
 * 受保护文章访问令牌服务端口。
 */
public interface ArticleAccessTokenService {

    /**
     * 为指定文章签发临时访问令牌。
     */
    ArticleAccessToken issue(int articleId);

    /**
     * 校验访问令牌是否允许访问指定文章。
     */
    boolean verify(int articleId, String token);
}
