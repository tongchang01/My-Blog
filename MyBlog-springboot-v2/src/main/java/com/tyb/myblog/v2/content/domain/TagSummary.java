package com.tyb.myblog.v2.content.domain;

/**
 * 标签摘要。
 *
 * @param id           标签 ID
 * @param name         标签名称
 * @param articleCount 该标签下已发布文章数量
 */
public record TagSummary(int id, String name, long articleCount) {
}
