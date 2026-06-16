package com.tyb.myblog.v2.content.application.article;

import com.tyb.myblog.v2.content.domain.article.ArticleStatus;

import java.time.LocalDateTime;

/**
 * 后台文章分页筛选条件。
 */
public record AdminArticleQuery(
        int page,
        int size,
        ArticleStatus status,
        Long categoryId,
        Long tagId,
        String titleKeyword,
        LocalDateTime createdFrom,
        LocalDateTime createdTo,
        LocalDateTime publishFrom,
        LocalDateTime publishTo) {
}
