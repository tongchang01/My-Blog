package com.tyb.myblog.v2.content.application.article;

/** 限制单个来源对单篇 PASSWORD 文章的解锁尝试。 */
public interface ArticleAccessRateLimitService {

    void checkAndRecord(String clientIp, long articleId);
}
