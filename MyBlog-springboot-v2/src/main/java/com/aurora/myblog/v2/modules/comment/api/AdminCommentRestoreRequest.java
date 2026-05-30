package com.aurora.myblog.v2.modules.comment.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AdminCommentRestoreRequest(
        @NotEmpty
        @Size(max = 100)
        List<Integer> ids
) {
}
