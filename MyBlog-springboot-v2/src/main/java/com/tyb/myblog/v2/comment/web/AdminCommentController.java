package com.tyb.myblog.v2.comment.web;

import com.tyb.myblog.v2.comment.application.AdminCommentCommandService;
import com.tyb.myblog.v2.comment.application.AdminCommentPageQuery;
import com.tyb.myblog.v2.comment.application.AdminCommentPageResult;
import com.tyb.myblog.v2.comment.application.AdminCommentQueryService;
import com.tyb.myblog.v2.comment.domain.CommentAuditStatus;
import com.tyb.myblog.v2.comment.domain.CommentTargetType;
import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.auth.CurrentUser;
import com.tyb.myblog.v2.common.web.ApiResponse;
import com.tyb.myblog.v2.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/comments")
@RequiredArgsConstructor
public class AdminCommentController {

    private final AdminCommentQueryService queryService;
    private final AdminCommentCommandService commandService;

    @Operation(summary = "分页查询后台评论")
    @GetMapping
    public ApiResponse<PageResponse<AdminCommentPageItemVO>> page(
            @CurrentUser AuthenticatedPrincipal principal,
            @RequestParam(required = false) CommentTargetType targetType,
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false) CommentAuditStatus auditStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        AdminCommentPageResult result = queryService.page(
                principal,
                new AdminCommentPageQuery(
                        targetType,
                        targetId,
                        auditStatus,
                        keyword,
                        includeDeleted,
                        page,
                        size));
        return ApiResponse.ok(new PageResponse<>(
                result.records().stream()
                        .map(AdminCommentPageItemVO::from)
                        .toList(),
                result.total(),
                result.page(),
                result.size()));
    }

    @Operation(summary = "审核通过评论")
    @PostMapping("/{id:\\d+}/approve")
    public ApiResponse<Void> approve(
            @CurrentUser AuthenticatedPrincipal principal,
            @PathVariable long id) {
        commandService.approve(principal, id);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "隐藏评论")
    @PostMapping("/{id:\\d+}/hide")
    public ApiResponse<Void> hide(
            @CurrentUser AuthenticatedPrincipal principal,
            @PathVariable long id) {
        commandService.hide(principal, id);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "恢复已删除评论")
    @PostMapping("/{id:\\d+}/restore")
    public ApiResponse<Void> restore(
            @CurrentUser AuthenticatedPrincipal principal,
            @PathVariable long id) {
        commandService.restore(principal, id);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/{id:\\d+}")
    public ApiResponse<Void> delete(
            @CurrentUser AuthenticatedPrincipal principal,
            @PathVariable long id) {
        commandService.delete(principal, id);
        return ApiResponse.ok(null);
    }
}
