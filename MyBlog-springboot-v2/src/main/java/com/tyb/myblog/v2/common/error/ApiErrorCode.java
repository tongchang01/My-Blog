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
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    /**
     * 用户名或密码错误，对应 HTTP 401。
     */
    BAD_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    /**
     * 当前接口需要登录但请求未提供有效身份，对应 HTTP 401。
     */
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED),
    /**
     * token 无效、过期或已撤销，对应 HTTP 401。
     */
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED),
    /**
     * 已认证但角色或权限不足，对应 HTTP 403。
     */
    FORBIDDEN(HttpStatus.FORBIDDEN),
    /**
     * 目标资源不存在或不可见，对应 HTTP 404。
     */
    NOT_FOUND(HttpStatus.NOT_FOUND),
    /**
     * 当前操作和已有业务状态冲突，对应 HTTP 409。
     */
    CONFLICT(HttpStatus.CONFLICT),
    /**
     * 未预期的服务端异常，对应 HTTP 500。
     */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ApiErrorCode(HttpStatus status) {
        this.status = status;
    }

    /**
     * 获取该错误码对应的 HTTP 状态。
     *
     * @return HTTP 状态
     */
    public HttpStatus status() {
        return status;
    }
}
