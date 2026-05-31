package com.tyb.myblog.v2.comment.web;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AdminCommentRestoreRequest(
        @NotEmpty
        @Size(max = 100)
        List<Integer> ids
) {
}
