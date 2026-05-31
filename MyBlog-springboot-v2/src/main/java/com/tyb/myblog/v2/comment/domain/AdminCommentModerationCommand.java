package com.tyb.myblog.v2.comment.domain;

import java.util.List;

public record AdminCommentModerationCommand(List<Integer> ids, boolean reviewed, int operatorUserId) {
}
