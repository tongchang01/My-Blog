package com.tyb.myblog.v2.common.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端 IP 解析工具。
 *
 * <p>优先读取反向代理写入的 {@code X-Forwarded-For}，其次读取 {@code X-Real-IP}，
 * 最后回退到 Servlet 容器看到的远端地址。该类只负责解析候选值，不负责判断代理链是否可信。</p>
 */
public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    /**
     * 解析当前请求的客户端 IP。
     *
     * @param request 当前请求
     * @return 解析后的 IP；当所有来源都为空时返回 {@code null}
     */
    public static String resolve(HttpServletRequest request) {
        String forwardedFor = firstForwardedIp(request.getHeader("X-Forwarded-For"));
        if (forwardedFor != null) {
            return forwardedFor;
        }
        String realIp = normalize(request.getHeader("X-Real-IP"));
        if (realIp != null) {
            return realIp;
        }
        return normalize(request.getRemoteAddr());
    }

    private static String firstForwardedIp(String value) {
        if (value == null) {
            return null;
        }
        // X-Forwarded-For 可能包含多级代理链，业务审计只记录最靠近客户端的第一个有效地址。
        for (String part : value.split(",")) {
            String ip = normalize(part);
            if (ip != null) {
                return ip;
            }
        }
        return null;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        // 请求头来自外部环境，先裁剪空白，避免审计字段出现不可见差异。
        return value.trim();
    }
}
