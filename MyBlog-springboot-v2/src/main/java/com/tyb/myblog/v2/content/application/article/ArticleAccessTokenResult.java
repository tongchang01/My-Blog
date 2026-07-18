package com.tyb.myblog.v2.content.application.article;

import java.time.LocalDateTime;

/** 解锁成功后只返回一次的明文令牌与过期时间。 */
public record ArticleAccessTokenResult(
        String token,
        LocalDateTime expiresAt) {
}
