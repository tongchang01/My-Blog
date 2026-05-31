package com.tyb.myblog.v2.content.domain;

import com.tyb.myblog.v2.common.web.PageResponse;

import java.util.Optional;

/**
 * 文章读取端口。
 *
 * <p>封装前台文章列表、详情、推荐和归档读取能力。实现层负责兼容旧库状态字段。</p>
 */
public interface ArticleReader {

    /**
     * 查询已发布文章列表。
     */
    PageResponse<ArticleSummary> listPublishedArticles(ArticlePageQuery query);

    /**
     * 按分类查询已发布文章列表。
     */
    PageResponse<ArticleSummary> listPublishedArticlesByCategory(int categoryId, ArticlePageQuery query);

    /**
     * 按标签查询已发布文章列表。
     */
    PageResponse<ArticleSummary> listPublishedArticlesByTag(int tagId, ArticlePageQuery query);

    /**
     * 查询首页置顶和推荐文章。
     */
    FeaturedArticles findFeaturedArticles();

    /**
     * 查询已发布文章归档。
     */
    PageResponse<ArchiveMonth> listPublishedArchives(ArticlePageQuery query);

    /**
     * 查询文章访问状态，用于判断公开、保护或不可见。
     */
    Optional<ArticleAccessCheck> findArticleAccessCheckById(int articleId);

    /**
     * 查询公开发布文章详情。
     */
    Optional<ArticleDetail> findPublishedArticleById(int articleId);

    /**
     * 查询已通过访问校验的文章详情。
     */
    Optional<ArticleDetail> findAccessibleArticleById(int articleId);
}
