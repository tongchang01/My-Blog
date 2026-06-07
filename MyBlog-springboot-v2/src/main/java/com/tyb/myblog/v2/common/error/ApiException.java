package com.tyb.myblog.v2.common.error;

/**
 * API 业务异常。
 *
 * <p>用于应用层或领域边界主动返回可预期的业务错误。
 * 全局异常处理器会根据错误码生成统一响应体。</p>
 */
public class ApiException extends RuntimeException {

    /**
     * 本次异常对应的统一 API 错误码。
     */
    private final ApiErrorCode code;

    /**
     * 使用错误码的中文默认消息创建业务异常。
     *
     * @param code 错误码
     */
    public ApiException(ApiErrorCode code) {
        this(code, code.defaultMessage());
    }

    /**
     * 创建业务异常。
     *
     * @param code    错误码
     * @param message 可返回给调用方的错误消息
     */
    public ApiException(ApiErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 获取错误码。
     *
     * @return API 错误码
     */
    public ApiErrorCode code() {
        return code;
    }
}
