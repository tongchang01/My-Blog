package com.tyb.myblog.v2.identity.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 后台登录请求。
 *
 * @param username 后台登录用户名
 * @param password 后台登录密码
 */
public record LoginRequest(
        @Schema(description = "后台登录用户名")
        @NotBlank(message = "用户名不能为空")
        @Size(max = 64, message = "用户名长度不能超过64个字符")
        String username,

        @Schema(description = "后台登录密码")
        @NotBlank(message = "密码不能为空")
        @Size(max = 128, message = "密码长度不能超过128个字符")
        String password
) {
}
