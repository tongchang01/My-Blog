package com.tyb.myblog.v2.comment.web;

import jakarta.validation.constraints.NotBlank;

public record AdminCommentReplyRequest(
        @NotBlank String contentMd) {
}
