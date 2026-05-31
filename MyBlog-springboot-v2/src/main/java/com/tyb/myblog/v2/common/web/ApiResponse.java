package com.tyb.myblog.v2.common.web;

/**
 * 统一 API 响应体。
 *
 * <p>所有 HTTP 接口使用同一响应结构，方便前台、后台和测试统一处理成功状态、错误码和业务数据。</p>
 *
 * @param success 请求是否成功
 * @param code    业务错误码；成功时固定为 {@code OK}
 * @param message 面向调用方展示或调试的消息
 * @param data    成功响应的数据载荷
 */
public record ApiResponse<T>(boolean success, String code, String message, T data) {

    /**
     * 创建成功响应。
     *
     * @param data 响应数据
     * @return 成功响应体
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", "success", data);
    }

    /**
     * 创建失败响应。
     *
     * @param code    业务错误码
     * @param message 错误消息
     * @return 失败响应体
     */
    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }
}
