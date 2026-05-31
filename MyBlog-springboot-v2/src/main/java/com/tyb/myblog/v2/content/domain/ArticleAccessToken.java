package com.tyb.myblog.v2.content.domain;

import java.time.Instant;

public record ArticleAccessToken(String value, Instant expiresAt) {
}
