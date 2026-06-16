package com.tyb.myblog.v2.content.application.article;

import com.tyb.myblog.v2.content.domain.article.PublicArticleCriteria;

import java.time.LocalDateTime;

public record PublicArticleQuery(
        int page,
        int size,
        String lang,
        Long categoryId,
        Long tagId,
        String keyword,
        String archiveMonth) {

    public PublicArticleCriteria toCriteria(LocalDateTime now) {
        return PublicArticleCriteria.from(
                page,
                size,
                categoryId,
                tagId,
                keyword,
                archiveMonth,
                now);
    }
}
