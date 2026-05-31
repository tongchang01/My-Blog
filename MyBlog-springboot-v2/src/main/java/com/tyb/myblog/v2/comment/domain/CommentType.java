package com.tyb.myblog.v2.comment.domain;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;

/**
 * 评论类型。
 *
 * <p>枚举值对应旧库 {@code t_comment.type} 数字编码。</p>
 */
public enum CommentType {
    /**
     * 文章评论，对应旧库编码 {@code 1}。
     */
    ARTICLE(1),
    /**
     * 留言板评论，对应旧库编码 {@code 2}。
     */
    MESSAGE(2),
    /**
     * 关于页评论，对应旧库编码 {@code 3}。
     */
    ABOUT(3),
    /**
     * 友链页评论，对应旧库编码 {@code 4}。
     */
    LINK(4),
    /**
     * 说说评论，对应旧库编码 {@code 5}。
     */
    TALK(5);

    private final int code;

    CommentType(int code) {
        this.code = code;
    }

    /**
     * 获取旧库数字编码。
     */
    public int code() {
        return code;
    }

    /**
     * 将旧库数字编码转换为评论类型。
     */
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

    /**
     * 是否必须绑定主题 ID。
     */
    public boolean requiresTopic() {
        return this == ARTICLE || this == TALK;
    }

    /**
     * 是否禁止绑定主题 ID。
     */
    public boolean forbidsTopic() {
        return this == MESSAGE || this == ABOUT || this == LINK;
    }
}
