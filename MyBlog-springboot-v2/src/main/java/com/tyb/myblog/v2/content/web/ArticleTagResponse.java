package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.domain.ArticleTagSummary;

/**
 * 文章标签响应。
 *
 * @param id   标签 ID
 * @param name 标签名称
 */
public record ArticleTagResponse(int id, String name) {

    static ArticleTagResponse from(ArticleTagSummary tag) {
        return new ArticleTagResponse(tag.id(), tag.name());
    }
}
