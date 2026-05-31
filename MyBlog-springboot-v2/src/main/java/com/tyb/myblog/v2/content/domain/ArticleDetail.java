package com.tyb.myblog.v2.content.domain;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章详情。
 *
 * @param id        文章 ID
 * @param title     文章标题
 * @param summary   文章摘要
 * @param content   文章正文
 * @param cover     封面地址
 * @param category  所属分类
 * @param author    作者信息
 * @param tags      标签列表
 * @param top       是否置顶
 * @param featured  是否推荐
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record ArticleDetail(
        int id,
        String title,
        String summary,
        String content,
        String cover,
        CategorySummary category,
        AuthorSummary author,
        List<ArticleTagSummary> tags,
        boolean top,
        boolean featured,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /**
     * 复制标签列表。
     */
    public ArticleDetail {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
