package com.tyb.myblog.v2.content.web;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;

/**
 * 标签完整写请求的 JSON presence 与字段规则校验。
 */
abstract class TagWriteRequestSupport {

    private SubmittedField<String> nameZh =
            SubmittedField.missing();
    private SubmittedField<String> nameJa =
            SubmittedField.missing();
    private SubmittedField<String> nameEn =
            SubmittedField.missing();
    private SubmittedField<String> slug =
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

    @JsonAnySetter
    public void rejectUnknown(String fieldName, JsonNode value) {
        throw new ApiException(
                ApiErrorCode.VALIDATION_ERROR,
                "不支持的标签字段: " + fieldName);
    }

    protected Values values() {
        requireAllFields();
        return new Values(
                nameZh.value(),
                nameJa.value(),
                nameEn.value(),
                slug.value());
    }

    private void requireAllFields() {
        if (!nameZh.submitted()
                || !nameJa.submitted()
                || !nameEn.submitted()
                || !slug.submitted()) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "请求必须包含全部标签字段");
        }
    }

    protected record Values(
            String nameZh,
            String nameJa,
            String nameEn,
            String slug) {
    }
}
