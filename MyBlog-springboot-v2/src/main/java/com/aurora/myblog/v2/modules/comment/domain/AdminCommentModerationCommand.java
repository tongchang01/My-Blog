package com.aurora.myblog.v2.modules.comment.domain;

import java.util.List;

public record AdminCommentModerationCommand(List<Integer> ids, boolean reviewed) {
}
