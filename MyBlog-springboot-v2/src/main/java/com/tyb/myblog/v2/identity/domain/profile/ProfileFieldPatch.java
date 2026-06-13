package com.tyb.myblog.v2.identity.domain.profile;

/**
 * 用户资料单字段补丁，区分未提交字段与显式提交空值。
 *
 * @param present 字段是否出现在更新请求中
 * @param value   字段提交值，显式清空时可为 {@code null}
 */
public record ProfileFieldPatch(boolean present, String value) {

    /**
     * 创建未提交字段。
     *
     * @return 未提交字段补丁
     */
    public static ProfileFieldPatch absent() {
        return new ProfileFieldPatch(false, null);
    }

    /**
     * 创建已提交字段。
     *
     * @param value 字段提交值
     * @return 已提交字段补丁
     */
    public static ProfileFieldPatch of(String value) {
        return new ProfileFieldPatch(true, value);
    }
}
