package com.tyb.myblog.v2.content.application.article;

import com.tyb.myblog.v2.content.domain.article.ArticleStatus;

/**
 * 文章访问统计所需的公开状态快照。
 */
public record ArticleStatisticsPolicySnapshot(
        long articleId,
        ArticleStatus status) {
}
