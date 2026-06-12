package com.tyb.myblog.v2.identity.application.auth;

/**
 * 后台登录成功后的双 token 结果。
 *
 * @param accessToken JWT access token
 * @param refreshToken 仅本次返回的 refresh token 明文
 * @param accessExpiresIn access token 有效秒数
 * @param refreshExpiresIn refresh token 有效秒数
 */
public record LoginTokenResult(
        String accessToken,
        String refreshToken,
        long accessExpiresIn,
        long refreshExpiresIn
) {
}
