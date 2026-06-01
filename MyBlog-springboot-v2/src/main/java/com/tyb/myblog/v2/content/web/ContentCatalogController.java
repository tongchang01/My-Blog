package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.common.web.ApiResponse;
import com.tyb.myblog.v2.content.application.ContentQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前台内容目录接口。
 *
 * <p>负责分类、标签和热门标签展示。</p>
 */
@RestController
public class ContentCatalogController {

    private final ContentQueryService contentQueryService;

    public ContentCatalogController(ContentQueryService contentQueryService) {
        this.contentQueryService = contentQueryService;
    }

    /**
     * 查询分类列表。
     */
    @GetMapping("/api/categories")
    public ApiResponse<List<CategoryResponse>> listCategories() {
        List<CategoryResponse> categories = contentQueryService.listCategories().stream()
                .map(CategoryResponse::from)
                .toList();
        return ApiResponse.ok(categories);
    }

    /**
     * 查询标签列表。
     */
    @GetMapping("/api/tags")
    public ApiResponse<List<TagResponse>> listTags() {
        List<TagResponse> tags = contentQueryService.listTags().stream()
                .map(TagResponse::from)
                .toList();
        return ApiResponse.ok(tags);
    }

    /**
     * 查询热门标签。
     */
    @GetMapping("/api/tags/top")
    public ApiResponse<List<TagResponse>> listTopTags(@RequestParam(required = false) Integer limit) {
        List<TagResponse> tags = contentQueryService.listTopTags(limit).stream()
                .map(TagResponse::from)
                .toList();
        return ApiResponse.ok(tags);
    }
}
