package com.aurora.myblog.v2.modules.comment.domain;

import java.util.List;

public record AdminCommentRestoreCommand(List<Integer> ids) {
}
