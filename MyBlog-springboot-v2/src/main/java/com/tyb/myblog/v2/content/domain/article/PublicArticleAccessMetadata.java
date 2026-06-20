package com.tyb.myblog.v2.content.domain.article;

/**
 * 公开文章详情读取前的最小访问元数据。
 */
public record PublicArticleAccessMetadata(
        long id,
        ArticleStatus status) {
}
