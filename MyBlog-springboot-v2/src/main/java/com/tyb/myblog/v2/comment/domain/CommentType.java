package com.tyb.myblog.v2.comment.domain;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;

public enum CommentType {
    ARTICLE(1),
    MESSAGE(2),
    ABOUT(3),
    LINK(4),
    TALK(5);

    private final int code;

    CommentType(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static CommentType fromCode(Integer code) {
        if (code == null) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "评论类型不能为空");
        }
        for (CommentType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "评论类型不支持");
    }

    public boolean requiresTopic() {
        return this == ARTICLE || this == TALK;
    }

    public boolean forbidsTopic() {
        return this == MESSAGE || this == ABOUT || this == LINK;
    }
}
