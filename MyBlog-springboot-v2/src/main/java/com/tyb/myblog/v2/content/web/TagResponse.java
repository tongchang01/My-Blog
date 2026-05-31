package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.domain.TagSummary;

/**
 * 标签响应。
 *
 * @param id           标签 ID
 * @param name         标签名称
 * @param articleCount 已发布文章数量
 */
public record TagResponse(int id, String name, long articleCount) {

    static TagResponse from(TagSummary tag) {
        return new TagResponse(tag.id(), tag.name(), tag.articleCount());
    }
}
