package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.common.web.ApiResponse;
import com.tyb.myblog.v2.common.web.PageResponse;
import com.tyb.myblog.v2.content.application.ContentQueryService;
import com.tyb.myblog.v2.content.domain.ArticleSummary;
import com.tyb.myblog.v2.content.domain.ArchiveMonth;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前台文章接口。
 *
 * <p>负责文章列表、详情、分类文章、标签文章、归档、推荐文章和受保护文章访问。</p>
 */
@RestController
public class ContentArticleController {

    private final ContentQueryService contentQueryService;

    public ContentArticleController(ContentQueryService contentQueryService) {
        this.contentQueryService = contentQueryService;
    }

    /**
     * 分页查询文章列表。
     */
    @GetMapping("/api/articles")
    public ApiResponse<PageResponse<ArticleSummaryResponse>> listArticles(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(mapArticlePage(contentQueryService.listArticles(page, size)));
    }

    /**
     * 查询首页推荐文章。
     */
    @GetMapping("/api/articles/featured")
    public ApiResponse<FeaturedArticlesResponse> getFeaturedArticles() {
        return ApiResponse.ok(FeaturedArticlesResponse.from(contentQueryService.getFeaturedArticles()));
    }

    /**
     * 分页查询文章归档。
     */
    @GetMapping("/api/articles/archives")
    public ApiResponse<PageResponse<ArchiveMonthResponse>> listArchives(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(mapArchivePage(contentQueryService.listArchives(page, size)));
    }

    /**
     * 校验受保护文章密码并获取临时访问令牌。
     */
    @PostMapping("/api/articles/{articleId}/access")
    public ApiResponse<ArticleAccessResponse> accessArticle(
            @PathVariable int articleId,
            @Valid @RequestBody ArticleAccessRequest request) {
        return ApiResponse.ok(ArticleAccessResponse.from(
                articleId,
                contentQueryService.accessProtectedArticle(articleId, request.password())));
    }

    /**
     * 查询文章详情。
     */
    @GetMapping("/api/articles/{articleId}")
    public ApiResponse<ArticleDetailResponse> getArticleDetail(
            @PathVariable int articleId,
            @RequestHeader(value = "X-Article-Access-Token", required = false) String accessToken) {
        return ApiResponse.ok(ArticleDetailResponse.from(contentQueryService.getArticleDetail(articleId, accessToken)));
    }

    /**
     * 按分类分页查询文章列表。
     */
    @GetMapping("/api/categories/{categoryId}/articles")
    public ApiResponse<PageResponse<ArticleSummaryResponse>> listArticlesByCategory(
            @PathVariable int categoryId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(mapArticlePage(contentQueryService.listArticlesByCategory(categoryId, page, size)));
    }

    /**
     * 按标签分页查询文章列表。
     */
    @GetMapping("/api/tags/{tagId}/articles")
    public ApiResponse<PageResponse<ArticleSummaryResponse>> listArticlesByTag(
            @PathVariable int tagId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(mapArticlePage(contentQueryService.listArticlesByTag(tagId, page, size)));
    }

    /**
     * 将领域分页转换为接口分页。
     */
    private PageResponse<ArticleSummaryResponse> mapArticlePage(PageResponse<ArticleSummary> page) {
        return new PageResponse<>(
                page.records().stream().map(ArticleSummaryResponse::from).toList(),
                page.total(),
                page.page(),
                page.size());
    }

    /**
     * 将归档分页转换为接口分页。
     */
    private PageResponse<ArchiveMonthResponse> mapArchivePage(PageResponse<ArchiveMonth> page) {
        return new PageResponse<>(
                page.records().stream().map(ArchiveMonthResponse::from).toList(),
                page.total(),
                page.page(),
                page.size());
    }
}
