package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.domain.ArticleSummary;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章摘要响应。
 *
 * @param id        文章 ID
 * @param title     标题
 * @param summary   摘要
 * @param cover     封面
 * @param category  分类
 * @param author    作者
 * @param tags      标签
 * @param top       是否置顶
 * @param featured  是否推荐
 * @param createdAt 创建时间
 */
public record ArticleSummaryResponse(
        int id,
        String title,
        String summary,
        String cover,
        ArticleCategoryResponse category,
        ArticleAuthorResponse author,
        List<ArticleTagResponse> tags,
        boolean top,
        boolean featured,
        LocalDateTime createdAt
) {

    /**
     * 从文章摘要领域对象转换为接口响应。
     */
    static ArticleSummaryResponse from(ArticleSummary article) {
        return new ArticleSummaryResponse(
                article.id(),
                article.title(),
                article.summary(),
                article.cover(),
                ArticleCategoryResponse.from(article.category()),
                ArticleAuthorResponse.from(article.author()),
                article.tags().stream().map(ArticleTagResponse::from).toList(),
                article.top(),
                article.featured(),
                article.createdAt());
    }
}
