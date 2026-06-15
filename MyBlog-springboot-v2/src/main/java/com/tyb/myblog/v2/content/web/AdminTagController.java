package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.auth.CurrentUser;
import com.tyb.myblog.v2.common.web.ApiResponse;
import com.tyb.myblog.v2.content.application.tag.TagCreateService;
import com.tyb.myblog.v2.content.application.tag.TagQueryService;
import com.tyb.myblog.v2.content.application.tag.TagUpdateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台标签只读接口。
 */
@Tag(name = "后台标签", description = "标签管理")
@RestController
@RequestMapping("/api/admin/tags")
@RequiredArgsConstructor
public class AdminTagController {

    private final TagQueryService queryService;
    private final TagCreateService createService;
    private final TagUpdateService updateService;
    private final TagWebMapping mapping;

    @GetMapping
    public ApiResponse<List<AdminTagVO>> list(
            @CurrentUser AuthenticatedPrincipal principal) {
        return ApiResponse.ok(
                queryService.adminList(principal).stream()
                        .map(mapping::toAdminVO)
                        .toList());
    }

    @GetMapping("/{id:\\d+}")
    public ApiResponse<AdminTagVO> detail(
            @CurrentUser AuthenticatedPrincipal principal,
            @PathVariable long id) {
        return ApiResponse.ok(mapping.toAdminVO(
                queryService.adminDetail(principal, id)));
    }

    @PostMapping
    @Operation(
            summary = "新增标签",
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            content = @Content(schema = @Schema(
                                    implementation =
                                            TagWriteOpenApiRequest.class))))
    public ApiResponse<AdminTagVO> create(
            @CurrentUser AuthenticatedPrincipal principal,
            @org.springframework.web.bind.annotation.RequestBody
            CreateTagRequest request) {
        return ApiResponse.ok(mapping.toAdminVO(
                createService.create(principal, request.toCommand())));
    }

    @PutMapping("/{id:\\d+}")
    @Operation(
            summary = "完整编辑标签",
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            content = @Content(schema = @Schema(
                                    implementation =
                                            TagWriteOpenApiRequest.class))))
    public ApiResponse<AdminTagVO> update(
            @CurrentUser AuthenticatedPrincipal principal,
            @PathVariable long id,
            @org.springframework.web.bind.annotation.RequestBody
            UpdateTagRequest request) {
        return ApiResponse.ok(mapping.toAdminVO(
                updateService.update(
                        principal, id, request.toCommand())));
    }
}
