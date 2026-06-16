package com.tyb.myblog.v2.content.application.article;

import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import com.tyb.myblog.v2.content.domain.article.DeletedArticlePage;

import java.time.LocalDateTime;
import java.util.List;

public record DeletedArticlePageResult(
        List<Item> records,
        long total,
        int page,
        int size) {

    public static DeletedArticlePageResult from(DeletedArticlePage page) {
        return new DeletedArticlePageResult(
                page.records().stream()
                        .map(item -> new Item(
                                item.id(),
                                item.titleZh(),
                                item.titleJa(),
                                item.titleEn(),
                                item.status(),
                                item.categoryId(),
                                item.deletedAt(),
                                item.deletedBy()))
                        .toList(),
                page.total(),
                page.page(),
                page.size());
    }

    public record Item(
            long id,
            String titleZh,
            String titleJa,
            String titleEn,
            ArticleStatus status,
            Long categoryId,
            LocalDateTime deletedAt,
            Long deletedBy) {
    }
}
