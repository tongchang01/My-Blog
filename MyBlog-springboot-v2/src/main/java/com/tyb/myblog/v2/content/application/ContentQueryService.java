package com.tyb.myblog.v2.content.application;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.common.web.PageResponse;
import com.tyb.myblog.v2.content.domain.ArticleAccessToken;
import com.tyb.myblog.v2.content.domain.ArticleAccessTokenService;
import com.tyb.myblog.v2.content.domain.ArticleDetail;
import com.tyb.myblog.v2.content.domain.ArticlePageQuery;
import com.tyb.myblog.v2.content.domain.ArticleReader;
import com.tyb.myblog.v2.content.domain.ArticleSummary;
import com.tyb.myblog.v2.content.domain.ArchiveMonth;
import com.tyb.myblog.v2.content.domain.CategorySummary;
import com.tyb.myblog.v2.content.domain.ContentCatalogReader;
import com.tyb.myblog.v2.content.domain.FeaturedArticles;
import com.tyb.myblog.v2.content.domain.TagSummary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
/**
 * 内容查询应用服务。
 *
 * <p>编排前台文章、分类、标签、归档、推荐文章和受保护文章访问。
 * 该类负责业务边界判断，不直接拼接 SQL。</p>
 */
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

    /**
     * 查询前台分类列表。
     */
    public List<CategorySummary> listCategories() {
        return catalogReader.listCategories();
    }

    /**
     * 查询前台标签列表。
     */
    public List<TagSummary> listTags() {
        return catalogReader.listTags();
    }

    /**
     * 查询热门标签列表，并限制最大返回数量。
     */
    public List<TagSummary> listTopTags(Integer limit) {
        return catalogReader.listTopTags(normalizeTopTagLimit(limit));
    }

    /**
     * 分页查询已发布文章。
     */
    public PageResponse<ArticleSummary> listArticles(Integer page, Integer size) {
        return articleReader.listPublishedArticles(ArticlePageQuery.of(page, size));
    }

    /**
     * 按分类分页查询已发布文章。
     */
    public PageResponse<ArticleSummary> listArticlesByCategory(int categoryId, Integer page, Integer size) {
        return articleReader.listPublishedArticlesByCategory(categoryId, ArticlePageQuery.of(page, size));
    }

    /**
     * 按标签分页查询已发布文章。
     */
    public PageResponse<ArticleSummary> listArticlesByTag(int tagId, Integer page, Integer size) {
        return articleReader.listPublishedArticlesByTag(tagId, ArticlePageQuery.of(page, size));
    }

    /**
     * 查询首页推荐文章。
     */
    public FeaturedArticles getFeaturedArticles() {
        return articleReader.findFeaturedArticles();
    }

    /**
     * 分页查询文章归档。
     */
    public PageResponse<ArchiveMonth> listArchives(Integer page, Integer size) {
        return articleReader.listPublishedArchives(ArticlePageQuery.of(page, size));
    }

    /**
     * 校验受保护文章访问密码并签发临时访问令牌。
     */
    public ArticleAccessToken accessProtectedArticle(int articleId, String password) {
        var check = articleReader.findArticleAccessCheckById(articleId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND, "文章不存在"));
        if (!check.protectedArticle()) {
            // 非受保护文章不通过密码接口暴露存在性，统一按不存在处理。
            throw new ApiException(ApiErrorCode.NOT_FOUND, "文章不存在");
        }
        if (!Objects.equals(check.password(), password)) {
            throw new ApiException(ApiErrorCode.FORBIDDEN, "文章访问密码错误");
        }
        return articleAccessTokenService.issue(articleId);
    }

    /**
     * 查询文章详情。
     *
     * <p>公开文章可直接访问；受保护文章必须提供有效访问令牌；
     * 草稿、删除或其他不可见状态统一按不存在处理。</p>
     */
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

    /**
     * 规范化热门标签数量，避免前端传入过大值拖慢查询。
     */
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
