package com.tyb.myblog.v2.identity.infrastructure.ratelimit;

import java.util.Locale;

/**
 * 登录限流缓存键。
 *
 * @param clientIp 规范化客户端 IP
 * @param username 规范化用户名
 */
record LoginRateLimitKey(String clientIp, String username) {

    private static final String UNKNOWN_IP = "<unknown>";

    LoginRateLimitKey {
        clientIp = normalizeIp(clientIp);
        username = normalizeUsername(username);
    }

    private static String normalizeIp(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN_IP;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeUsername(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("登录限流用户名不能为空");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
