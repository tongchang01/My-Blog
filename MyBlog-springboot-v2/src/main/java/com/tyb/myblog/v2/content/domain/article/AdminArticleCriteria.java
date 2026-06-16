package com.tyb.myblog.v2.content.domain.article;

import java.time.LocalDateTime;

/**
 * 后台文章仓储查询条件，保持 domain 端口不依赖 application。
 */
public record AdminArticleCriteria(
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
