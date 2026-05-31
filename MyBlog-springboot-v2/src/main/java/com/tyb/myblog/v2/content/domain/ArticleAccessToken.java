package com.tyb.myblog.v2.content.domain;

import java.time.Instant;

/**
 * 受保护文章临时访问令牌。
 *
 * @param value     令牌值
 * @param expiresAt 过期时间
 */
public record ArticleAccessToken(String value, Instant expiresAt) {
}
