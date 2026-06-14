package com.tyb.myblog.v2.identity.application.auth;

/**
 * 当前 ADMIN 修改本人密码的应用命令。
 *
 * @param currentPassword 当前明文密码
 * @param newPassword 新明文密码
 */
public record ChangePasswordCommand(
        String currentPassword,
        String newPassword
) {
}
