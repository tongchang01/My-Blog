package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.common.web.ApiResponse;
import com.tyb.myblog.v2.content.application.category.CategoryQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公开分类查询接口。
 */
@Tag(name = "公开分类", description = "按语言读取分类")
@RestController
@RequestMapping("/api/public/categories")
@RequiredArgsConstructor
public class PublicCategoryController {

    private final CategoryQueryService queryService;
    private final CategoryWebMapping mapping;

    @Operation(summary = "查询公开分类列表")
    @GetMapping
    public ApiResponse<List<PublicCategoryVO>> list(
            @RequestParam String lang) {
        return ApiResponse.ok(
                queryService.publicList(lang).stream()
                        .map(mapping::toPublicVO)
                        .toList());
    }
}
