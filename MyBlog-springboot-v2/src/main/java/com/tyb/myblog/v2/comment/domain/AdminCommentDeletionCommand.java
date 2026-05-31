package com.tyb.myblog.v2.comment.domain;

import java.util.List;

public record AdminCommentDeletionCommand(List<Integer> ids, int operatorUserId) {
}
