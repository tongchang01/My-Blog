package com.tyb.myblog.v2.system.web;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.auth.CurrentUser;
import com.tyb.myblog.v2.common.web.ApiResponse;
import com.tyb.myblog.v2.common.web.PageResponse;
import com.tyb.myblog.v2.system.application.friendlink.FriendLinkPageResult;
import com.tyb.myblog.v2.system.application.friendlink.FriendLinkCreateService;
import com.tyb.myblog.v2.system.application.friendlink.FriendLinkQueryService;
import com.tyb.myblog.v2.system.application.friendlink.FriendLinkUpdateService;
import com.tyb.myblog.v2.system.application.friendlink.FriendLinkStatusService;
import com.tyb.myblog.v2.system.application.friendlink.FriendLinkSortService;
import com.tyb.myblog.v2.system.application.friendlink.FriendLinkDeleteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台友链管理接口。
 */
@Tag(name = "后台友链", description = "友链管理")
@RestController
@RequestMapping("/api/admin/friend-links")
@RequiredArgsConstructor
public class AdminFriendLinkController {

    private final FriendLinkQueryService queryService;
    private final FriendLinkCreateService createService;
    private final FriendLinkUpdateService updateService;
    private final FriendLinkStatusService statusService;
    private final FriendLinkSortService sortService;
    private final FriendLinkDeleteService deleteService;

    @Operation(summary = "分页查询后台友链")
    @GetMapping
    public ApiResponse<PageResponse<AdminFriendLinkVO>> page(
            @CurrentUser AuthenticatedPrincipal principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        FriendLinkPageResult result =
                queryService.adminPage(principal, page, size);
        return ApiResponse.ok(new PageResponse<>(
                result.records().stream()
                        .map(AdminFriendLinkVO::from)
                        .toList(),
                result.total(),
                result.page(),
                result.size()));
    }

    @Operation(summary = "查询后台友链详情")
    @GetMapping("/{id:\\d+}")
    public ApiResponse<AdminFriendLinkVO> detail(
            @CurrentUser AuthenticatedPrincipal principal,
            @PathVariable long id) {
        return ApiResponse.ok(AdminFriendLinkVO.from(
                queryService.adminDetail(principal, id)));
    }

    @PostMapping
    @Operation(
            summary = "新增友链",
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            content = @Content(schema = @Schema(
                                    implementation =
                                            FriendLinkWriteOpenApiRequest.class))))
    public ApiResponse<AdminFriendLinkVO> create(
            @CurrentUser AuthenticatedPrincipal principal,
            @org.springframework.web.bind.annotation.RequestBody
            CreateFriendLinkRequest request) {
        return ApiResponse.ok(AdminFriendLinkVO.from(
                createService.create(principal, request.toCommand())));
    }

    @PutMapping("/{id:\\d+}")
    @Operation(
            summary = "完整编辑友链",
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            content = @Content(schema = @Schema(
                                    implementation =
                                            FriendLinkWriteOpenApiRequest.class))))
    public ApiResponse<AdminFriendLinkVO> update(
            @CurrentUser AuthenticatedPrincipal principal,
            @PathVariable long id,
            @org.springframework.web.bind.annotation.RequestBody
            UpdateFriendLinkRequest request) {
        return ApiResponse.ok(AdminFriendLinkVO.from(
                updateService.update(
                        principal, id, request.toCommand())));
    }

    @Operation(summary = "修改友链可见状态")
    @PatchMapping("/{id:\\d+}/status")
    public ApiResponse<AdminFriendLinkVO> updateStatus(
            @CurrentUser AuthenticatedPrincipal principal,
            @PathVariable long id,
            @org.springframework.web.bind.annotation.RequestBody
            UpdateFriendLinkStatusRequest request) {
        return ApiResponse.ok(AdminFriendLinkVO.from(
                statusService.update(
                        principal, id, request.toCommand())));
    }

    @Operation(summary = "批量调整友链排序")
    @PutMapping("/sort-orders")
    public ApiResponse<java.util.List<AdminFriendLinkVO>>
            updateSortOrders(
                    @CurrentUser AuthenticatedPrincipal principal,
                    @org.springframework.web.bind.annotation.RequestBody
                    UpdateFriendLinkSortOrdersRequest request) {
        return ApiResponse.ok(sortService.update(
                        principal, request.toCommand())
                .stream()
                .map(AdminFriendLinkVO::from)
                .toList());
    }

    @Operation(summary = "删除友链")
    @DeleteMapping("/{id:\\d+}")
    public ApiResponse<Void> delete(
            @CurrentUser AuthenticatedPrincipal principal,
            @PathVariable long id) {
        deleteService.delete(principal, id);
        return ApiResponse.ok(null);
    }
}
