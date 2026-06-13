package com.tyb.myblog.v2.identity.web;

import jakarta.validation.constraints.NotBlank;

/**
 * 刷新认证会话请求。
 */
public record RefreshTokenRequest(
        @NotBlank String refreshToken
) {
}
