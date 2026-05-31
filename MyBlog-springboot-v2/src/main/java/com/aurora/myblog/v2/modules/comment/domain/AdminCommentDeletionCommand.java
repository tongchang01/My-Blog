package com.aurora.myblog.v2.modules.comment.domain;

import java.util.List;

public record AdminCommentDeletionCommand(List<Integer> ids, int operatorUserId) {
}
