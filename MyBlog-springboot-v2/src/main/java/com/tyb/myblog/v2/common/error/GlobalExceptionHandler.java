package com.tyb.myblog.v2.common.error;

import com.tyb.myblog.v2.common.web.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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
                ? "internal server error"
                : exception.getMessage();
        return ResponseEntity.status(exception.code().status())
                .body(ApiResponse.fail(exception.code().name(), message));
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
                .body(ApiResponse.fail(ApiErrorCode.VALIDATION_ERROR.name(), message));
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
                .body(ApiResponse.fail(ApiErrorCode.VALIDATION_ERROR.name(), "malformed request body"));
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
                .body(ApiResponse.fail(ApiErrorCode.INTERNAL_ERROR.name(), "internal server error"));
    }
}
