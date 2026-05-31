package com.tyb.myblog.v2.content.domain;

/**
 * 分类摘要。
 *
 * @param id           分类 ID
 * @param name         分类名称
 * @param articleCount 该分类下已发布文章数量
 */
public record CategorySummary(int id, String name, long articleCount) {
}
