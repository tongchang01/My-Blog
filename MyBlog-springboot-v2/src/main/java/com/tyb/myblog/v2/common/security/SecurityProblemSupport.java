package com.tyb.myblog.v2.common.security;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.web.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;

/**
 * Spring Security 异常响应写入器。
 *
 * <p>Spring Security 的认证失败和授权失败默认不会进入 MVC 全局异常处理器。
 * 该类负责把这类安全异常也写成统一的 {@link ApiResponse} JSON 结构。</p>
 */
public class SecurityProblemSupport {

    /**
     * JSON 序列化器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 创建安全异常响应写入器。
     *
     * @param objectMapper JSON 序列化器
     */
    public SecurityProblemSupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 写入未登录响应。
     *
     * @param response 当前 HTTP 响应
     */
    public void writeUnauthorized(HttpServletResponse response) throws IOException {
        ApiErrorCode errorCode = ApiErrorCode.INVALID_TOKEN;
        write(response, errorCode.status().value(),
                ApiResponse.fail(errorCode.code(), errorCode.defaultMessage()));
    }

    /**
     * 写入权限不足响应。
     *
     * @param response 当前 HTTP 响应
     */
    public void writeForbidden(HttpServletResponse response) throws IOException {
        ApiErrorCode errorCode = ApiErrorCode.FORBIDDEN;
        write(response, errorCode.status().value(),
                ApiResponse.fail(errorCode.code(), errorCode.defaultMessage()));
    }

    /**
     * 按统一格式写入安全错误响应。
     */
    private void write(HttpServletResponse response, int status, ApiResponse<Void> body) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
