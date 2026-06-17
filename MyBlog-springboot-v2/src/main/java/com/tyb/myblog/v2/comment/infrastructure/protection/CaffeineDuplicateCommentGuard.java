package com.tyb.myblog.v2.comment.infrastructure.protection;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tyb.myblog.v2.comment.application.DuplicateCommentGuard;
import com.tyb.myblog.v2.comment.domain.CommentTarget;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

@Service
public class CaffeineDuplicateCommentGuard implements DuplicateCommentGuard {

    private final Cache<String, Boolean> recentSubmissions = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(50_000)
            .build();

    @Override
    public void checkAndRecord(
            String clientIp,
            CommentTarget target,
            String contentMd) {
        String key = normalizeIp(clientIp)
                + "|"
                + target.targetType().databaseValue()
                + "|"
                + target.targetId()
                + "|"
                + sha256(contentMd == null ? "" : contentMd.trim());
        Boolean existing = recentSubmissions.asMap().putIfAbsent(key, Boolean.TRUE);
        if (existing != null) {
            throw new ApiException(ApiErrorCode.CONFLICT, "请勿重复提交相同评论");
        }
    }

    private static String normalizeIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return "unknown";
        }
        return clientIp.trim();
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", ex);
        }
    }
}
