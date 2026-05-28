package com.aurora.myblog.v2.modules.content.application;

import com.aurora.myblog.v2.common.web.PageResponse;
import com.aurora.myblog.v2.modules.content.domain.ArticlePageQuery;
import com.aurora.myblog.v2.modules.content.domain.ArticleReader;
import com.aurora.myblog.v2.modules.content.domain.ArticleSummary;
import com.aurora.myblog.v2.modules.content.domain.CategorySummary;
import com.aurora.myblog.v2.modules.content.domain.ContentCatalogReader;
import com.aurora.myblog.v2.modules.content.domain.TagSummary;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContentQueryService {

    private static final int DEFAULT_TOP_TAG_LIMIT = 10;
    private static final int MAX_TOP_TAG_LIMIT = 50;

    private final ContentCatalogReader catalogReader;
    private final ArticleReader articleReader;

    public ContentQueryService(ContentCatalogReader catalogReader, ArticleReader articleReader) {
        this.catalogReader = catalogReader;
        this.articleReader = articleReader;
    }

    public List<CategorySummary> listCategories() {
        return catalogReader.listCategories();
    }

    public List<TagSummary> listTags() {
        return catalogReader.listTags();
    }

    public List<TagSummary> listTopTags(Integer limit) {
        return catalogReader.listTopTags(normalizeTopTagLimit(limit));
    }

    public PageResponse<ArticleSummary> listArticles(Integer page, Integer size) {
        return articleReader.listPublishedArticles(ArticlePageQuery.of(page, size));
    }

    public PageResponse<ArticleSummary> listArticlesByCategory(int categoryId, Integer page, Integer size) {
        return articleReader.listPublishedArticlesByCategory(categoryId, ArticlePageQuery.of(page, size));
    }

    public PageResponse<ArticleSummary> listArticlesByTag(int tagId, Integer page, Integer size) {
        return articleReader.listPublishedArticlesByTag(tagId, ArticlePageQuery.of(page, size));
    }

    private int normalizeTopTagLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_TOP_TAG_LIMIT;
        }
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, MAX_TOP_TAG_LIMIT);
    }
}
