package com.tyb.myblog.v2.content.web;

/**
 * 公开标签响应。
 */
public record PublicTagVO(
        long id,
        String name,
        String slug) {
}
