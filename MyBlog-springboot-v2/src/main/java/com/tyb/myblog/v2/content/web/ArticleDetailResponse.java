package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.domain.ArticleDetail;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章详情响应。
 *
 * @param id        文章 ID
 * @param title     标题
 * @param summary   摘要
 * @param content   正文
 * @param cover     封面
 * @param category  分类
 * @param author    作者
 * @param tags      标签
 * @param top       是否置顶
 * @param featured  是否推荐
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record ArticleDetailResponse(
        int id,
        String title,
        String summary,
        String content,
        String cover,
        ArticleCategoryResponse category,
        ArticleAuthorResponse author,
        List<ArticleTagResponse> tags,
        boolean top,
        boolean featured,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /**
     * 从文章详情领域对象转换为接口响应。
     */
    static ArticleDetailResponse from(ArticleDetail article) {
        return new ArticleDetailResponse(
                article.id(),
                article.title(),
                article.summary(),
                article.content(),
                article.cover(),
                ArticleCategoryResponse.from(article.category()),
                ArticleAuthorResponse.from(article.author()),
                article.tags().stream().map(ArticleTagResponse::from).toList(),
                article.top(),
                article.featured(),
                article.createdAt(),
                article.updatedAt());
    }
}
