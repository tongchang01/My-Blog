package com.tyb.myblog.v2.content.web;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.domain.article.ArticleSlug;
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章完整写请求的 JSON presence 与基础字段校验。
 */
abstract class ArticleWriteRequestSupport {

    private SubmittedField<String> titleZh = SubmittedField.missing();
    private SubmittedField<String> titleJa = SubmittedField.missing();
    private SubmittedField<String> titleEn = SubmittedField.missing();
    private SubmittedField<String> summaryZh = SubmittedField.missing();
    private SubmittedField<String> summaryJa = SubmittedField.missing();
    private SubmittedField<String> summaryEn = SubmittedField.missing();
    private SubmittedField<String> body = SubmittedField.missing();
    private SubmittedField<Long> categoryId = SubmittedField.missing();
    private SubmittedField<List<Long>> tagIds = SubmittedField.missing();
    private SubmittedField<String> slug = SubmittedField.missing();
    private SubmittedField<ArticleStatus> status = SubmittedField.missing();
    private SubmittedField<String> password = SubmittedField.missing();
    private SubmittedField<LocalDateTime> publishAt = SubmittedField.missing();
    private SubmittedField<Long> coverAttachmentId = SubmittedField.missing();

    @JsonSetter("titleZh")
    public void setTitleZh(String value) {
        titleZh = SubmittedField.present(value);
    }

    @JsonSetter("titleJa")
    public void setTitleJa(String value) {
        titleJa = SubmittedField.present(value);
    }

    @JsonSetter("titleEn")
    public void setTitleEn(String value) {
        titleEn = SubmittedField.present(value);
    }

    @JsonSetter("summaryZh")
    public void setSummaryZh(String value) {
        summaryZh = SubmittedField.present(value);
    }

    @JsonSetter("summaryJa")
    public void setSummaryJa(String value) {
        summaryJa = SubmittedField.present(value);
    }

    @JsonSetter("summaryEn")
    public void setSummaryEn(String value) {
        summaryEn = SubmittedField.present(value);
    }

    @JsonSetter("body")
    public void setBody(String value) {
        body = SubmittedField.present(value);
    }

    @JsonSetter("categoryId")
    public void setCategoryId(Long value) {
        categoryId = SubmittedField.present(value);
    }

    @JsonSetter("tagIds")
    public void setTagIds(List<Long> value) {
        tagIds = SubmittedField.present(value);
    }

    @JsonSetter("slug")
    public void setSlug(String value) {
        slug = SubmittedField.present(value);
    }

    @JsonSetter("status")
    public void setStatus(ArticleStatus value) {
        status = SubmittedField.present(value);
    }

    @JsonSetter("password")
    public void setPassword(String value) {
        password = SubmittedField.present(value);
    }

    @JsonSetter("publishAt")
    public void setPublishAt(LocalDateTime value) {
        publishAt = SubmittedField.present(value);
    }

    @JsonSetter("coverAttachmentId")
    public void setCoverAttachmentId(Long value) {
        coverAttachmentId = SubmittedField.present(value);
    }

    @JsonAnySetter
    public void rejectUnknown(String fieldName, JsonNode value) {
        throw validation("不支持的文章字段: " + fieldName);
    }

    protected Values values() {
        requireAllFields();
        if (status.value() == null) {
            throw validation("文章状态不能为空");
        }
        try {
            String normalizedSlug = ArticleSlug.optional(slug.value())
                    .map(ArticleSlug::value)
                    .orElse(null);
            return new Values(
                    titleZh.value(),
                    titleJa.value(),
                    titleEn.value(),
                    summaryZh.value(),
                    summaryJa.value(),
                    summaryEn.value(),
                    body.value(),
                    categoryId.value(),
                    normalizeTagIds(tagIds.value()),
                    normalizedSlug,
                    status.value(),
                    password.value(),
                    publishAt.value(),
                    coverAttachmentId.value());
        } catch (IllegalArgumentException exception) {
            throw validation(exception.getMessage());
        }
    }

    void requireAllFields() {
        if (!titleZh.submitted()
                || !titleJa.submitted()
                || !titleEn.submitted()
                || !summaryZh.submitted()
                || !summaryJa.submitted()
                || !summaryEn.submitted()
                || !body.submitted()
                || !categoryId.submitted()
                || !tagIds.submitted()
                || !slug.submitted()
                || !status.submitted()
                || !password.submitted()
                || !publishAt.submitted()
                || !coverAttachmentId.submitted()) {
            throw validation("文章请求字段不完整");
        }
    }

    private List<Long> normalizeTagIds(List<Long> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        if (raw.size() > 20) {
            throw new IllegalArgumentException(
                    "文章标签数量不能超过 20");
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

    private ApiException validation(String message) {
        return new ApiException(
                ApiErrorCode.VALIDATION_ERROR,
                message);
    }

    protected record Values(
            String titleZh,
            String titleJa,
            String titleEn,
            String summaryZh,
            String summaryJa,
            String summaryEn,
            String body,
            Long categoryId,
            List<Long> tagIds,
            String slug,
            ArticleStatus status,
            String password,
            LocalDateTime publishAt,
            Long coverAttachmentId) {
    }
}
