package com.tyb.myblog.v2.content.domain.article;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 文章重建和写入共用的字段规范化与状态约束。
 */
final class ArticleValidation {

    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_SUMMARY_LENGTH = 500;
    private static final int MAX_PASSWORD_HASH_LENGTH = 255;
    private static final int MAX_TAGS = 20;

    private ArticleValidation() {
    }

    static ArticleValues validateStored(ArticleValues raw) {
        Objects.requireNonNull(raw, "文章字段不能为空");
        ArticleValues values = normalize(raw);
        validateCommon(values);
        return values;
    }

    static ArticleValues validateForWrite(
            ArticleValues raw,
            LocalDateTime now) {
        Objects.requireNonNull(now, "当前时间不能为空");
        ArticleValues values = validateStored(raw);
        if (values.status() == ArticleStatus.SCHEDULED
                && values.publishAt().isBefore(now)) {
            throw new IllegalArgumentException(
                    "定时发布时间不得早于当前时间");
        }
        return values;
    }

    private static ArticleValues normalize(ArticleValues raw) {
        String titleZh = optionalText(
                raw.titleZh(), MAX_TITLE_LENGTH, "中文标题");
        String titleJa = optionalText(
                raw.titleJa(), MAX_TITLE_LENGTH, "日文标题");
        String titleEn = optionalText(
                raw.titleEn(), MAX_TITLE_LENGTH, "英文标题");
        String summaryZh = optionalText(
                raw.summaryZh(), MAX_SUMMARY_LENGTH, "中文摘要");
        String summaryJa = optionalText(
                raw.summaryJa(), MAX_SUMMARY_LENGTH, "日文摘要");
        String summaryEn = optionalText(
                raw.summaryEn(), MAX_SUMMARY_LENGTH, "英文摘要");
        String body = raw.body() == null || raw.body().isBlank()
                ? null
                : raw.body();
        String slug = ArticleSlug.optional(raw.slug())
                .map(ArticleSlug::value)
                .orElse(null);
        String password = optionalText(
                raw.accessPassword(),
                MAX_PASSWORD_HASH_LENGTH,
                "文章密码哈希");
        List<Long> tagIds = normalizeTagIds(raw.tagIds());
        return new ArticleValues(
                titleZh,
                titleJa,
                titleEn,
                summaryZh,
                summaryJa,
                summaryEn,
                body,
                raw.categoryId(),
                raw.authorId(),
                slug,
                Objects.requireNonNull(raw.status(), "文章状态不能为空"),
                password,
                raw.publishAt(),
                raw.coverAttachmentId(),
                raw.commentCount(),
                tagIds);
    }

    private static void validateCommon(ArticleValues values) {
        if (values.authorId() <= 0) {
            throw new IllegalArgumentException("文章作者 ID 必须为正数");
        }
        if (values.categoryId() != null && values.categoryId() <= 0) {
            throw new IllegalArgumentException("文章分类 ID 必须为正数");
        }
        if (values.coverAttachmentId() != null
                && values.coverAttachmentId() <= 0) {
            throw new IllegalArgumentException("封面附件 ID 必须为正数");
        }
        if (values.commentCount() < 0) {
            throw new IllegalArgumentException("评论数不能为负数");
        }
        if (values.status() != ArticleStatus.DRAFT) {
            requirePresent(values.titleZh(), "中文标题");
            requirePresent(values.body(), "正文");
            if (values.categoryId() == null) {
                throw new IllegalArgumentException(
                        "当前文章状态必须选择分类");
            }
        }
        if (values.status() == ArticleStatus.PASSWORD) {
            requirePresent(values.accessPassword(), "文章密码哈希");
        } else if (values.accessPassword() != null) {
            throw new IllegalArgumentException(
                    "非密码文章不得保留密码哈希");
        }
        if ((values.status() == ArticleStatus.PUBLISHED
                || values.status() == ArticleStatus.PASSWORD
                || values.status() == ArticleStatus.SCHEDULED)
                && values.publishAt() == null) {
            throw new IllegalArgumentException(
                    "当前文章状态必须设置发布时间");
        }
    }

    private static String optionalText(
            String raw,
            int maxLength,
            String fieldName) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + "长度超过限制");
        }
        return normalized;
    }

    private static List<Long> normalizeTagIds(List<Long> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        if (raw.size() > MAX_TAGS) {
            throw new IllegalArgumentException(
                    "文章标签数量不能超过 " + MAX_TAGS);
        }
        List<Long> normalized = raw.stream()
                .peek(id -> {
                    if (id == null || id <= 0) {
                        throw new IllegalArgumentException(
                                "文章标签 ID 必须为正数");
                    }
                })
                .distinct()
                .sorted()
                .toList();
        if (normalized.size() != raw.size()) {
            throw new IllegalArgumentException("文章标签 ID 不得重复");
        }
        return normalized;
    }

    private static void requirePresent(
            String value,
            String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
    }

    record ArticleValues(
            String titleZh,
            String titleJa,
            String titleEn,
            String summaryZh,
            String summaryJa,
            String summaryEn,
            String body,
            Long categoryId,
            long authorId,
            String slug,
            ArticleStatus status,
            String accessPassword,
            LocalDateTime publishAt,
            Long coverAttachmentId,
            int commentCount,
            List<Long> tagIds) {
    }
}
