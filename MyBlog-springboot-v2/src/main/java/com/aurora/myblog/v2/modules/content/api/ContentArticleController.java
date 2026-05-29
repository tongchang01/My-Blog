package com.aurora.myblog.v2.modules.content.api;

import com.aurora.myblog.v2.common.web.ApiResponse;
import com.aurora.myblog.v2.common.web.PageResponse;
import com.aurora.myblog.v2.modules.content.domain.ArticleSummary;
import com.aurora.myblog.v2.modules.content.application.ContentQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ContentArticleController {

    private final ContentQueryService contentQueryService;

    public ContentArticleController(ContentQueryService contentQueryService) {
        this.contentQueryService = contentQueryService;
    }

    @GetMapping("/api/articles")
    public ApiResponse<PageResponse<ArticleSummaryResponse>> listArticles(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(mapArticlePage(contentQueryService.listArticles(page, size)));
    }

    @GetMapping("/api/articles/{articleId}")
    public ApiResponse<ArticleDetailResponse> getArticleDetail(@PathVariable int articleId) {
        return ApiResponse.ok(ArticleDetailResponse.from(contentQueryService.getArticleDetail(articleId)));
    }

    @GetMapping("/api/categories/{categoryId}/articles")
    public ApiResponse<PageResponse<ArticleSummaryResponse>> listArticlesByCategory(
            @PathVariable int categoryId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(mapArticlePage(contentQueryService.listArticlesByCategory(categoryId, page, size)));
    }

    @GetMapping("/api/tags/{tagId}/articles")
    public ApiResponse<PageResponse<ArticleSummaryResponse>> listArticlesByTag(
            @PathVariable int tagId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(mapArticlePage(contentQueryService.listArticlesByTag(tagId, page, size)));
    }

    private PageResponse<ArticleSummaryResponse> mapArticlePage(PageResponse<ArticleSummary> page) {
        return new PageResponse<>(
                page.records().stream().map(ArticleSummaryResponse::from).toList(),
                page.total(),
                page.page(),
                page.size());
    }
}
