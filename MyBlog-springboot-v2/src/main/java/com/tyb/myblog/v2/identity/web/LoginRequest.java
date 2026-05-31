package com.tyb.myblog.v2.identity.web;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求。
 *
 * @param username 登录用户名
 * @param password 登录密码，禁止记录日志
 */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {
}
