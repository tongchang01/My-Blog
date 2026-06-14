package com.tyb.myblog.v2.system.domain.friendlink;

import java.net.URI;

/**
 * 友链领域字段的统一校验与归一化。
 */
final class FriendLinkValidation {

    static final int MAX_NAME_LENGTH = 64;
    static final int MAX_URL_LENGTH = 255;
    static final int MAX_DESCRIPTION_LENGTH = 255;
    static final int MAX_SORT_ORDER = 1_000_000;

    private FriendLinkValidation() {
    }

    static Values validate(
            String name,
            String url,
            String avatarUrl,
            String description,
            int sortOrder,
            FriendLinkStatus status) {
        String normalizedName = requiredText(
                name, MAX_NAME_LENGTH, "友链名称");
        String normalizedUrl = httpUrl(url, true, "友链地址");
        String normalizedAvatar = httpUrl(
                avatarUrl, false, "友链头像地址");
        String normalizedDescription = optionalText(
                description, MAX_DESCRIPTION_LENGTH, "友链介绍");
        if (sortOrder < 0 || sortOrder > MAX_SORT_ORDER) {
            throw new IllegalArgumentException(
                    "友链排序值必须在0到1000000之间");
        }
        if (status == null) {
            throw new IllegalArgumentException("友链状态不能为空");
        }
        return new Values(
                normalizedName,
                normalizedUrl,
                normalizedAvatar,
                normalizedDescription,
                sortOrder,
                status);
    }

    private static String requiredText(
            String value,
            int maxLength,
            String fieldName) {
        String normalized = optionalText(value, maxLength, fieldName);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return normalized;
    }

    private static String optionalText(
            String value,
            int maxLength,
            String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + "长度不能超过" + maxLength);
        }
        return normalized;
    }

    private static String httpUrl(
            String value,
            boolean required,
            String fieldName) {
        String normalized = optionalText(
                value, MAX_URL_LENGTH, fieldName);
        if (normalized == null) {
            if (required) {
                throw new IllegalArgumentException(
                        fieldName + "不能为空");
            }
            return null;
        }
        try {
            URI uri = URI.create(normalized);
            boolean supportedScheme =
                    "http".equalsIgnoreCase(uri.getScheme())
                            || "https".equalsIgnoreCase(
                                    uri.getScheme());
            if (!supportedScheme
                    || uri.getHost() == null
                    || uri.getHost().isBlank()
                    || uri.getUserInfo() != null) {
                throw invalidUrl(fieldName, null);
            }
            return normalized;
        } catch (IllegalArgumentException exception) {
            if (exception.getCause() == null
                    && exception.getMessage() != null
                    && exception.getMessage().startsWith(fieldName)) {
                throw exception;
            }
            throw invalidUrl(fieldName, exception);
        }
    }

    private static IllegalArgumentException invalidUrl(
            String fieldName,
            IllegalArgumentException cause) {
        return new IllegalArgumentException(
                fieldName + "必须是有效的HTTP或HTTPS绝对地址",
                cause);
    }

    record Values(
            String name,
            String url,
            String avatarUrl,
            String description,
            int sortOrder,
            FriendLinkStatus status
    ) {
    }
}
