package com.aurora.myblog.v2.modules.content.api;

import jakarta.validation.constraints.NotBlank;

public record ArticleAccessRequest(@NotBlank(message = "password must not be blank") String password) {
}
