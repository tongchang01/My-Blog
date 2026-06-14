package com.tyb.myblog.v2.system.domain.attachment;

import com.tyb.myblog.v2.common.storage.StorageType;

import java.net.URI;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 附件领域字段的统一规范化与校验。
 */
final class AttachmentValidation {

    static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final int MAX_SIDE = 20_000;
    private static final long MAX_PIXELS = 40_000_000L;
    private static final Pattern HASH = Pattern.compile("^[0-9a-f]{64}$");

    private AttachmentValidation() {
    }

    static Values validate(
            StorageType storageType,
            String bucket,
            String objectKey,
            String publicUrl,
            String contentType,
            long fileSize,
            int width,
            int height,
            String originalFilename,
            String hashSha256) {
        Objects.requireNonNull(storageType, "附件存储类型不能为空");
        String normalizedBucket = optional(bucket, "存储桶", 64);
        String normalizedKey = required(objectKey, "对象键", 255);
        validateObjectKey(normalizedKey);
        String normalizedUrl = absoluteUrl(publicUrl);
        String normalizedContentType =
                required(contentType, "文件类型", 64);
        String normalizedHash = required(hashSha256, "文件摘要", 64);
        if (!HASH.matcher(normalizedHash).matches()) {
            throw new IllegalArgumentException(
                    "文件摘要必须是64位小写十六进制");
        }
        if (fileSize < 1 || fileSize > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "附件大小必须在1字节到10 MiB之间");
        }
        validateDimensions(width, height);
        return new Values(
                storageType,
                normalizedBucket,
                normalizedKey,
                normalizedUrl,
                normalizedContentType,
                fileSize,
                width,
                height,
                filename(originalFilename),
                normalizedHash);
    }

    private static void validateObjectKey(String objectKey) {
        if (objectKey.startsWith("/")
                || objectKey.startsWith("\\")
                || objectKey.contains("\\")
                || Pattern.compile("(^|/)\\.\\.?(?:/|$)")
                .matcher(objectKey).find()) {
            throw new IllegalArgumentException("附件对象键格式错误");
        }
    }

    private static String absoluteUrl(String value) {
        String normalized = required(value, "附件公开地址", 512);
        URI uri;
        try {
            uri = URI.create(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("附件公开地址格式错误", exception);
        }
        if (uri.getHost() == null
                || (!"http".equalsIgnoreCase(uri.getScheme())
                && !"https".equalsIgnoreCase(uri.getScheme()))) {
            throw new IllegalArgumentException(
                    "附件公开地址仅支持绝对 HTTP 或 HTTPS URL");
        }
        return normalized;
    }

    private static void validateDimensions(int width, int height) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("图片宽高必须为正数");
        }
        if (width > MAX_SIDE || height > MAX_SIDE) {
            throw new IllegalArgumentException("图片单边不能超过20000像素");
        }
        long pixels;
        try {
            pixels = Math.multiplyExact((long) width, height);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("图片总像素过大", exception);
        }
        if (pixels > MAX_PIXELS) {
            throw new IllegalArgumentException("图片总像素不能超过40000000");
        }
    }

    private static String filename(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String withoutControl = value.replaceAll("\\p{Cntrl}", "");
        String normalizedPath = withoutControl.replace('\\', '/');
        String name = normalizedPath.substring(
                normalizedPath.lastIndexOf('/') + 1).trim();
        if (name.isEmpty()) {
            return null;
        }
        if (name.length() > 255) {
            throw new IllegalArgumentException("原始文件名不能超过255个字符");
        }
        return name;
    }

    private static String required(
            String value,
            String field,
            int maxLength) {
        String normalized = optional(value, field, maxLength);
        if (normalized == null) {
            throw new IllegalArgumentException(field + "不能为空");
        }
        return normalized;
    }

    private static String optional(
            String value,
            String field,
            int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    field + "不能超过" + maxLength + "个字符");
        }
        return normalized;
    }

    record Values(
            StorageType storageType,
            String bucket,
            String objectKey,
            String publicUrl,
            String contentType,
            long fileSize,
            int width,
            int height,
            String originalFilename,
            String hashSha256) {
    }
}
