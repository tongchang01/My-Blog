package com.tyb.myblog.v2.common.web;

/**
 * 统一 API 响应体。
 *
 * @param code 5 位业务错误码，成功时固定为 {@code 00000}
 * @param msg 人类可读消息
 * @param data 成功响应的数据载荷，失败时为 {@code null}
 */
public record ApiResponse<T>(String code, String msg, T data) {

    /**
     * 创建成功响应。
     *
     * @param data 响应数据
     * @return 成功响应体
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("00000", "success", data);
    }

    /**
     * 创建失败响应。
     *
     * @param code    业务错误码
     * @param msg 错误消息
     * @return 失败响应体
     */
    public static ApiResponse<Void> fail(String code, String msg) {
        return new ApiResponse<>(code, msg, null);
    }
}
