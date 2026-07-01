package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.common.web.ApiResponse;
import com.tyb.myblog.v2.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import com.tyb.myblog.v2.content.application.article.PublicArticleQuery;
import com.tyb.myblog.v2.content.application.article.PublicArticleQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开文章列表与详情接口，PASSWORD 文章只公开锁定元数据。
 */
@RestController
@RequestMapping("/api/public/articles")
@RequiredArgsConstructor
public class PublicArticleController {

    private final PublicArticleQueryService queryService;
    private final ArticleWebMapping mapping;

    @Operation(summary = "分页查询公开文章")
    @GetMapping
    public ApiResponse<PageResponse<PublicArticlePageItemVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "zh") String lang,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String archiveMonth) {
        return ApiResponse.ok(mapping.toPublicPage(
                queryService.page(new PublicArticleQuery(
                        page,
                        size,
                        lang,
                        categoryId,
                        tagId,
                        keyword,
                        archiveMonth))));
    }

    @Operation(summary = "查询公开首页文章")
    @GetMapping("/home")
    public ApiResponse<PublicArticleHomeVO> home(
            @RequestParam(defaultValue = "zh") String lang,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(mapping.toPublicHome(
                queryService.home(lang, size)));
    }

    @Operation(summary = "查询公开文章详情")
    @GetMapping("/{id:\\d+}")
    public ApiResponse<PublicArticleDetailVO> detail(
            @PathVariable long id,
            @RequestParam(defaultValue = "zh") String lang) {
        return ApiResponse.ok(mapping.toPublicDetail(
                queryService.detail(id, lang)));
    }
}
