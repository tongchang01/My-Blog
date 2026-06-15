package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.auth.CurrentUser;
import com.tyb.myblog.v2.common.web.ApiResponse;
import com.tyb.myblog.v2.content.application.category.CategoryQueryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台分类只读接口。
 */
@Tag(name = "后台分类", description = "分类管理")
@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryQueryService queryService;
    private final CategoryWebMapping mapping;

    @GetMapping
    public ApiResponse<List<AdminCategoryVO>> list(
            @CurrentUser AuthenticatedPrincipal principal) {
        return ApiResponse.ok(
                queryService.adminList(principal).stream()
                        .map(mapping::toAdminVO)
                        .toList());
    }

    @GetMapping("/{id:\\d+}")
    public ApiResponse<AdminCategoryVO> detail(
            @CurrentUser AuthenticatedPrincipal principal,
            @PathVariable long id) {
        return ApiResponse.ok(mapping.toAdminVO(
                queryService.adminDetail(principal, id)));
    }
}
