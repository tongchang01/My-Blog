package com.tyb.myblog.v2.content.domain;

/**
 * 分类或标签 slug 已被占用。
 */
public class ContentSlugConflictException extends RuntimeException {

    public ContentSlugConflictException() {
        super("slug 已被占用");
    }
}
