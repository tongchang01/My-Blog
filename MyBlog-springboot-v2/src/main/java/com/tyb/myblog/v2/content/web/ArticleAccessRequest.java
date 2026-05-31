package com.tyb.myblog.v2.content.web;

import jakarta.validation.constraints.NotBlank;

public record ArticleAccessRequest(@NotBlank(message = "password must not be blank") String password) {
}
