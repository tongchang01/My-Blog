package com.tyb.myblog.v2.system.web;

/**
 * 保存 JSON 字段是否出现以及对应值。
 */
record SubmittedField<T>(boolean present, T value) {

    static <T> SubmittedField<T> absent() {
        return new SubmittedField<>(false, null);
    }

    static <T> SubmittedField<T> of(T value) {
        return new SubmittedField<>(true, value);
    }
}
