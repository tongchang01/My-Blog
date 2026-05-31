package com.tyb.myblog.v2.common.web;

import jakarta.servlet.http.HttpServletRequest;

public final class UserAgentResolver {

    private static final int MAX_LENGTH = 255;

    private UserAgentResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isBlank()) {
            return null;
        }
        String normalized = userAgent.trim();
        if (normalized.length() <= MAX_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_LENGTH);
    }
}
