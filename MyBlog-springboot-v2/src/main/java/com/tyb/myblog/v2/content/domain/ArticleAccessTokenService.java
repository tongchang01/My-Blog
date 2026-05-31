package com.tyb.myblog.v2.content.domain;

public interface ArticleAccessTokenService {

    ArticleAccessToken issue(int articleId);

    boolean verify(int articleId, String token);
}
