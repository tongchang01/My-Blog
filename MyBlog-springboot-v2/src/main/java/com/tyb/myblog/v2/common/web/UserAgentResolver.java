package com.tyb.myblog.v2.common.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端 User-Agent 解析工具。
 *
 * <p>用于登录审计、评论审计等需要记录客户端环境的场景。
 * 该工具只做空值处理、去除首尾空白和长度截断，不判断浏览器或设备类型。</p>
 */
public final class UserAgentResolver {

    /**
     * User-Agent 最大入库长度。
     *
     * <p>当前限制为 255，避免旧库或审计表字段长度较短时被异常请求撑爆。</p>
     */
    private static final int MAX_LENGTH = 255;

    private UserAgentResolver() {
    }

    /**
     * 从 HTTP 请求中解析可用于审计的 User-Agent。
     *
     * @param request 当前请求
     * @return 规范化后的 User-Agent；请求头为空时返回 {@code null}
     */
    public static String resolve(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isBlank()) {
            return null;
        }
        String normalized = userAgent.trim();
        if (normalized.length() <= MAX_LENGTH) {
            return normalized;
        }
        // User-Agent 由客户端提供，不可信且可能很长，入库前必须截断。
        return normalized.substring(0, MAX_LENGTH);
    }
}
