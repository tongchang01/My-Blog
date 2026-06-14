package com.tyb.myblog.v2.common.error;

import com.tyb.myblog.v2.common.web.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局 API 异常处理器。
 *
 * <p>负责把业务异常、参数校验异常和未预期异常转换为统一响应体。
 * 这里是接口错误信息的最后出口，不能把服务端内部堆栈或敏感实现细节暴露给调用方。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理应用主动抛出的业务异常。
     *
     * @param exception 业务异常
     * @return 统一错误响应
     */
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiResponse<Void>> handleApiException(ApiException exception) {
        // 5xx 错误统一隐藏原始消息，避免把内部实现细节返回给前端。
        String message = exception.code().status().is5xxServerError()
                ? exception.code().defaultMessage()
                : exception.getMessage();
        return ResponseEntity.status(exception.code().status())
                .body(ApiResponse.fail(exception.code().code(), message));
    }

    /**
     * 处理 Bean Validation 参数校验异常。
     *
     * @param exception 参数校验异常
     * @return 统一错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse("request validation failed");
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ApiErrorCode.VALIDATION_ERROR.code(), message));
    }

    /**
     * 处理请求体无法解析的异常。
     *
     * @param exception 请求体解析异常
     * @return 统一错误响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiResponse<Void>> handleUnreadableMessageException(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ApiErrorCode.VALIDATION_ERROR.code(), "请求体格式错误"));
    }

    /**
     * 处理 URL 存在但 HTTP 方法不受支持的请求。
     *
     * @param exception HTTP 方法不匹配异常
     * @return HTTP 405 统一错误响应
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiResponse<Void>> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .body(ApiResponse.fail(
                        ApiErrorCode.VALIDATION_ERROR.code(),
                        "请求方法不支持"));
    }

    /**
     * 处理超过统一附件大小限制的 multipart 请求。
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeException(
            MaxUploadSizeExceededException exception) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(
                        ApiErrorCode.VALIDATION_ERROR.code(),
                        "上传文件不能超过10 MiB"));
    }

    /**
     * 处理缺少 multipart 文件字段的请求。
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    ResponseEntity<ApiResponse<Void>> handleMissingRequestPartException(
            MissingServletRequestPartException exception) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(
                        ApiErrorCode.VALIDATION_ERROR.code(),
                        "缺少上传文件"));
    }

    /**
     * 处理未预期异常。
     *
     * @param exception 原始异常
     * @return 统一错误响应
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        log.error("Unhandled API exception", exception);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.fail(
                        ApiErrorCode.INTERNAL_ERROR.code(),
                        ApiErrorCode.INTERNAL_ERROR.defaultMessage()));
    }
}
