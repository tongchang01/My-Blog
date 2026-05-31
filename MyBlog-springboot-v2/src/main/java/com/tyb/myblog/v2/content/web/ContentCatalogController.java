package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.common.web.ApiResponse;
import com.tyb.myblog.v2.content.application.ContentQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ContentCatalogController {

    private final ContentQueryService contentQueryService;

    public ContentCatalogController(ContentQueryService contentQueryService) {
        this.contentQueryService = contentQueryService;
    }

    @GetMapping("/api/categories")
    public ApiResponse<List<CategoryResponse>> listCategories() {
        List<CategoryResponse> categories = contentQueryService.listCategories().stream()
                .map(CategoryResponse::from)
                .toList();
        return ApiResponse.ok(categories);
    }

    @GetMapping("/api/tags")
    public ApiResponse<List<TagResponse>> listTags() {
        List<TagResponse> tags = contentQueryService.listTags().stream()
                .map(TagResponse::from)
                .toList();
        return ApiResponse.ok(tags);
    }

    @GetMapping("/api/tags/top")
    public ApiResponse<List<TagResponse>> listTopTags(@RequestParam(required = false) Integer limit) {
        List<TagResponse> tags = contentQueryService.listTopTags(limit).stream()
                .map(TagResponse::from)
                .toList();
        return ApiResponse.ok(tags);
    }
}
