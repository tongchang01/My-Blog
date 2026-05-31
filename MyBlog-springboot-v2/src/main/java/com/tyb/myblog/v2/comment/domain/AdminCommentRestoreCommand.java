package com.tyb.myblog.v2.comment.domain;

import java.util.List;

public record AdminCommentRestoreCommand(List<Integer> ids, int operatorUserId) {
}
