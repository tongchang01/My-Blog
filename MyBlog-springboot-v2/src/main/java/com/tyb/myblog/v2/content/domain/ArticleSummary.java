package com.tyb.myblog.v2.content.domain;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章摘要。
 *
 * @param id        文章 ID
 * @param title     文章标题
 * @param summary   文章摘要
 * @param cover     封面地址
 * @param category  所属分类
 * @param author    作者信息
 * @param tags      标签列表
 * @param top       是否置顶
 * @param featured  是否推荐
 * @param createdAt 创建时间
 */
public record ArticleSummary(
        int id,
        String title,
        String summary,
        String cover,
        CategorySummary category,
        AuthorSummary author,
        List<ArticleTagSummary> tags,
        boolean top,
        boolean featured,
        LocalDateTime createdAt
) {

    /**
     * 复制标签列表。
     */
    public ArticleSummary {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
