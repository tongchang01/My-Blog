package com.tyb.myblog.v2.content.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** PASSWORD 文章解锁请求。 */
public record PublicArticleUnlockRequest(
        @NotBlank(message = "文章密码不能为空")
        @Size(max = 200, message = "文章密码长度不能超过 200")
        String password) {
}
