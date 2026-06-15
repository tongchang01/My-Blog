package com.tyb.myblog.v2.content.web;

/**
 * 公开分类响应。
 */
public record PublicCategoryVO(
        long id,
        String name,
        String slug) {
}
