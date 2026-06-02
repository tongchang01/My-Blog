package com.tyb.myblog.v2.common.auth;

import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Bearer Token 解析器。
 *
 * <p>统一处理 {@code Authorization} 请求头，避免过滤器、Controller 各自写字符串截取逻辑。
 * 当前只接受标准 {@code Bearer token} 格式，前缀大小写、额外空格或空 token 都视为无效。</p>
 */
@Component
public class BearerTokenResolver {

    /**
     * 标准 Bearer 前缀。
     */
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 从 Authorization 请求头中解析访问令牌。
     *
     * @param authorization Authorization 请求头原始值
     * @return 解析出的 token；请求头为空或格式不标准时返回空
     */
    public Optional<String> resolve(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        String token = authorization.substring(BEARER_PREFIX.length());
        if (token.isBlank() || token.startsWith(" ")) {
            return Optional.empty();
        }
        return Optional.of(token);
    }
}
