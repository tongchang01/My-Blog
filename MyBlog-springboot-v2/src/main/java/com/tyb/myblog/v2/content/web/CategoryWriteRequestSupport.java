package com.tyb.myblog.v2.content.web;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.domain.ContentName;
import com.tyb.myblog.v2.content.domain.ContentSlug;

/**
 * 分类完整写请求的 JSON presence 与字段规则校验。
 */
abstract class CategoryWriteRequestSupport {

    private SubmittedField<String> nameZh =
            SubmittedField.missing();
    private SubmittedField<String> nameJa =
            SubmittedField.missing();
    private SubmittedField<String> nameEn =
            SubmittedField.missing();
    private SubmittedField<String> slug =
            SubmittedField.missing();
    private SubmittedField<Integer> sortOrder =
            SubmittedField.missing();

    @JsonSetter("nameZh")
    public void setNameZh(String value) {
        nameZh = SubmittedField.present(value);
    }

    @JsonSetter("nameJa")
    public void setNameJa(String value) {
        nameJa = SubmittedField.present(value);
    }

    @JsonSetter("nameEn")
    public void setNameEn(String value) {
        nameEn = SubmittedField.present(value);
    }

    @JsonSetter("slug")
    public void setSlug(String value) {
        slug = SubmittedField.present(value);
    }

    @JsonSetter("sortOrder")
    public void setSortOrder(Integer value) {
        sortOrder = SubmittedField.present(value);
    }

    @JsonAnySetter
    public void rejectUnknown(String fieldName, JsonNode value) {
        throw new ApiException(
                ApiErrorCode.VALIDATION_ERROR,
                "不支持的分类字段: " + fieldName);
    }

    protected Values values() {
        requireAllFields();
        if (sortOrder.value() == null) {
            throw validation("分类排序值不能为空");
        }
        try {
            ContentName name = ContentName.of(
                    nameZh.value(), nameJa.value(), nameEn.value());
            ContentSlug normalizedSlug =
                    ContentSlug.of(slug.value());
            if (sortOrder.value() < 0
                    || sortOrder.value() > 1_000_000) {
                throw new IllegalArgumentException(
                        "分类排序值必须在 0 到 1000000 之间");
            }
            return new Values(
                    name.zh(),
                    name.ja(),
                    name.en(),
                    normalizedSlug.value(),
                    sortOrder.value());
        } catch (IllegalArgumentException exception) {
            throw validation(exception.getMessage());
        }
    }

    private void requireAllFields() {
        if (!nameZh.submitted()
                || !nameJa.submitted()
                || !nameEn.submitted()
                || !slug.submitted()
                || !sortOrder.submitted()) {
            throw validation("请求必须包含全部分类字段");
        }
    }

    private ApiException validation(String message) {
        return new ApiException(
                ApiErrorCode.VALIDATION_ERROR,
                message);
    }

    protected record Values(
            String nameZh,
            String nameJa,
            String nameEn,
            String slug,
            int sortOrder) {
    }
}
