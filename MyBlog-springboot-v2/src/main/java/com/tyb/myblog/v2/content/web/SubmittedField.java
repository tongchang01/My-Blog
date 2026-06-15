package com.tyb.myblog.v2.content.web;

/**
 * 保存 JSON 字段是否提交以及对应值。
 */
record SubmittedField<T>(boolean submitted, T value) {

    static <T> SubmittedField<T> missing() {
        return new SubmittedField<>(false, null);
    }

    static <T> SubmittedField<T> present(T value) {
        return new SubmittedField<>(true, value);
    }
}
