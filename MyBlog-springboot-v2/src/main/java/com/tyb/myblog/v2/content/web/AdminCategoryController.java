package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.auth.CurrentUser;
import com.tyb.myblog.v2.common.web.ApiResponse;
import com.tyb.myblog.v2.content.application.category.CategoryCreateService;
import com.tyb.myblog.v2.content.application.category.CategoryDeleteService;
import com.tyb.myblog.v2.content.application.category.CategoryQueryService;
import com.tyb.myblog.v2.content.application.category.CategorySortService;
import com.tyb.myblog.v2.content.application.category.CategoryUpdateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    private final CategoryCreateService createService;
    private final CategoryUpdateService updateService;
    private final CategorySortService sortService;
    private final CategoryDeleteService deleteService;
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

    @PostMapping
    @Operation(
            summary = "新增分类",
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            content = @Content(schema = @Schema(
                                    implementation =
                                            CategoryWriteOpenApiRequest.class))))
    public ApiResponse<AdminCategoryVO> create(
            @CurrentUser AuthenticatedPrincipal principal,
            @org.springframework.web.bind.annotation.RequestBody
            CreateCategoryRequest request) {
        return ApiResponse.ok(mapping.toAdminVO(
                createService.create(principal, request.toCommand())));
    }

    @PutMapping("/{id:\\d+}")
    @Operation(
            summary = "完整编辑分类",
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            content = @Content(schema = @Schema(
                                    implementation =
                                            CategoryWriteOpenApiRequest.class))))
    public ApiResponse<AdminCategoryVO> update(
            @CurrentUser AuthenticatedPrincipal principal,
            @PathVariable long id,
            @org.springframework.web.bind.annotation.RequestBody
            UpdateCategoryRequest request) {
        return ApiResponse.ok(mapping.toAdminVO(
                updateService.update(
                        principal, id, request.toCommand())));
    }

    @PutMapping("/sort-orders")
    public ApiResponse<Void> updateSortOrders(
            @CurrentUser AuthenticatedPrincipal principal,
            @org.springframework.web.bind.annotation.RequestBody
            UpdateCategorySortOrdersRequest request) {
        sortService.update(principal, request.toCommand());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id:\\d+}")
    public ApiResponse<Void> delete(
            @CurrentUser AuthenticatedPrincipal principal,
            @PathVariable long id) {
        deleteService.delete(principal, id);
        return ApiResponse.ok(null);
    }
}
