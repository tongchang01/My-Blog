package com.tyb.myblog.v2.identity.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 后台登录成功响应。
 *
 * @param accessToken JWT access token
 * @param refreshToken 仅本次返回的 refresh token 明文
 * @param accessExpiresIn access token 有效秒数
 * @param refreshExpiresIn refresh token 有效秒数
 */
public record LoginTokenVO(
        @Schema(description = "JWT access token")
        String accessToken,
        @Schema(description = "仅本次返回的 refresh token 明文")
        String refreshToken,
        @Schema(description = "access token 有效秒数")
        long accessExpiresIn,
        @Schema(description = "refresh token 有效秒数")
        long refreshExpiresIn
) {
}
