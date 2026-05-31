package com.tyb.myblog.v2.content.web;

import jakarta.validation.constraints.NotBlank;

/**
 * 受保护文章访问请求。
 *
 * @param password 文章访问密码
 */
public record ArticleAccessRequest(@NotBlank(message = "password must not be blank") String password) {
}
