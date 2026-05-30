package com.aurora.myblog.v2.modules.content.application;

import com.aurora.myblog.v2.common.error.ApiErrorCode;
import com.aurora.myblog.v2.common.error.ApiException;
import com.aurora.myblog.v2.common.web.PageResponse;
import com.aurora.myblog.v2.modules.content.domain.ArticleAccessToken;
import com.aurora.myblog.v2.modules.content.domain.ArticleAccessTokenService;
import com.aurora.myblog.v2.modules.content.domain.ArticleDetail;
import com.aurora.myblog.v2.modules.content.domain.ArticlePageQuery;
import com.aurora.myblog.v2.modules.content.domain.ArticleReader;
import com.aurora.myblog.v2.modules.content.domain.ArticleSummary;
import com.aurora.myblog.v2.modules.content.domain.ArchiveMonth;
import com.aurora.myblog.v2.modules.content.domain.CategorySummary;
import com.aurora.myblog.v2.modules.content.domain.ContentCatalogReader;
import com.aurora.myblog.v2.modules.content.domain.FeaturedArticles;
import com.aurora.myblog.v2.modules.content.domain.TagSummary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class ContentQueryService {

    private static final int DEFAULT_TOP_TAG_LIMIT = 10;
    private static final int MAX_TOP_TAG_LIMIT = 50;

    private final ContentCatalogReader catalogReader;
    private final ArticleReader articleReader;
    private final ArticleAccessTokenService articleAccessTokenService;

    public ContentQueryService(ContentCatalogReader catalogReader,
                               ArticleReader articleReader,
                               ArticleAccessTokenService articleAccessTokenService) {
        this.catalogReader = catalogReader;
        this.articleReader = articleReader;
        this.articleAccessTokenService = articleAccessTokenService;
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

    public FeaturedArticles getFeaturedArticles() {
        return articleReader.findFeaturedArticles();
    }

    public PageResponse<ArchiveMonth> listArchives(Integer page, Integer size) {
        return articleReader.listPublishedArchives(ArticlePageQuery.of(page, size));
    }

    public ArticleAccessToken accessProtectedArticle(int articleId, String password) {
        var check = articleReader.findArticleAccessCheckById(articleId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND, "文章不存在"));
        if (!check.protectedArticle()) {
            throw new ApiException(ApiErrorCode.NOT_FOUND, "文章不存在");
        }
        if (!Objects.equals(check.password(), password)) {
            throw new ApiException(ApiErrorCode.FORBIDDEN, "文章访问密码错误");
        }
        return articleAccessTokenService.issue(articleId);
    }

    public ArticleDetail getArticleDetail(int articleId, String accessToken) {
        var check = articleReader.findArticleAccessCheckById(articleId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND, "文章不存在"));
        if (check.publicArticle()) {
            return articleReader.findPublishedArticleById(articleId)
                    .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND, "文章不存在"));
        }
        if (!check.protectedArticle()) {
            throw new ApiException(ApiErrorCode.NOT_FOUND, "文章不存在");
        }
        if (!articleAccessTokenService.verify(articleId, accessToken)) {
            throw new ApiException(ApiErrorCode.FORBIDDEN, "文章访问令牌无效");
        }
        return articleReader.findAccessibleArticleById(articleId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND, "文章不存在"));
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
