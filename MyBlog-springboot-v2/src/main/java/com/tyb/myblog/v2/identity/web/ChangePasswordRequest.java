package com.tyb.myblog.v2.identity.web;

import com.tyb.myblog.v2.identity.application.auth.ChangePasswordCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 当前用户修改密码请求。
 *
 * @param currentPassword 当前密码，原样参与 BCrypt 校验
 * @param newPassword 新密码，长度为 8 至 128 个字符
 */
public record ChangePasswordRequest(
        @Schema(
                description = "当前密码",
                accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank(message = "当前密码不能为空")
        @Size(
                max = 128,
                message = "当前密码长度不能超过128个字符")
        String currentPassword,

        @Schema(
                description = "新密码",
                accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank(message = "新密码不能为空")
        @Size(
                min = 8,
                max = 128,
                message = "新密码长度必须为8至128个字符")
        String newPassword
) {

    /**
     * 转换为不包含 HTTP 校验注解的应用命令。
     *
     * @return 修改密码命令
     */
    public ChangePasswordCommand toCommand() {
        return new ChangePasswordCommand(
                currentPassword,
                newPassword);
    }
}
