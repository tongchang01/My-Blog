package com.aurora.myblog.v2.modules.content.domain;

public interface ArticleAccessTokenService {

    ArticleAccessToken issue(int articleId);

    boolean verify(int articleId, String token);
}
