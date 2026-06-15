package com.tyb.myblog.v2.content.application.tag;

/**
 * 公开标签查询结果。
 */
public record PublicTagResult(
        long id,
        String name,
        String slug) {
}
