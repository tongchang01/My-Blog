package com.tyb.myblog.v2.common.error;

import org.springframework.http.HttpStatus;

/**
 * API 统一错误码。
 *
 * <p>错误码用于前台、后台识别失败类型，并映射为稳定的 HTTP 状态码。
 * 新增错误码时应优先表达业务语义，避免把内部异常名称直接暴露给调用方。</p>
 */
public enum ApiErrorCode {
    /**
     * 请求参数校验失败，对应 HTTP 400。
     */
    VALIDATION_ERROR("90001", HttpStatus.BAD_REQUEST, "参数校验失败"),
    /**
     * 请求超过基础设施限流阈值，对应 HTTP 429。
     */
    RATE_LIMITED("90002", HttpStatus.TOO_MANY_REQUESTS, "请求过于频繁"),
    /**
     * 用户名或密码错误，对应 HTTP 401。
     */
    BAD_CREDENTIALS("10001", HttpStatus.UNAUTHORIZED, "用户名或密码错误"),
    /**
     * 未提供 token，或 token 无效、过期、已撤销、版本不匹配，对应 HTTP 401。
     */
    INVALID_TOKEN("10002", HttpStatus.UNAUTHORIZED, "登录状态已失效"),
    /**
     * 已认证但角色或权限不足，对应 HTTP 403。
     */
    FORBIDDEN("10003", HttpStatus.FORBIDDEN, "无权执行当前操作"),
    /**
     * 目标资源不存在或不可见，对应 HTTP 404。
     */
    NOT_FOUND("90003", HttpStatus.NOT_FOUND, "目标资源不存在"),
    /**
     * 当前操作和已有业务状态冲突，对应 HTTP 409。
     */
    CONFLICT("90004", HttpStatus.CONFLICT, "当前操作与已有状态冲突"),
    /**
     * 未预期的服务端异常，对应 HTTP 500。
     */
    INTERNAL_ERROR("99999", HttpStatus.INTERNAL_SERVER_ERROR, "系统内部错误");

    private final String code;
    private final HttpStatus status;
    private final String defaultMessage;

    ApiErrorCode(String code, HttpStatus status, String defaultMessage) {
        this.code = code;
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    /**
     * 获取返回给前端的稳定错误码。
     *
     * @return 5 位业务错误码
     */
    public String code() {
        return code;
    }

    /**
     * 获取该错误码对应的 HTTP 状态。
     *
     * @return HTTP 状态
     */
    public HttpStatus status() {
        return status;
    }

    /**
     * 获取中文兜底消息。
     *
     * @return 中文默认消息
     */
    public String defaultMessage() {
        return defaultMessage;
    }
}
