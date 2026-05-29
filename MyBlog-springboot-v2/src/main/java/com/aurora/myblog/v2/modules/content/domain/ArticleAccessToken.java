package com.aurora.myblog.v2.modules.content.domain;

import java.time.Instant;

public record ArticleAccessToken(String value, Instant expiresAt) {
}
