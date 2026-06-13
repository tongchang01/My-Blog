package com.tyb.myblog.v2.identity.application.profile;

/**
 * 应用边界的部分更新值，区分字段未出现与显式提交空值。
 *
 * @param present 字段是否出现在请求中
 * @param value   字段提交值
 * @param <T>     字段值类型
 */
public record PatchValue<T>(boolean present, T value) {

    /**
     * 创建未出现的字段值。
     *
     * @param <T> 字段值类型
     * @return 未出现字段值
     */
    public static <T> PatchValue<T> absent() {
        return new PatchValue<>(false, null);
    }

    /**
     * 创建已出现的字段值。
     *
     * @param value 字段提交值
     * @param <T>   字段值类型
     * @return 已出现字段值
     */
    public static <T> PatchValue<T> of(T value) {
        return new PatchValue<>(true, value);
    }
}
