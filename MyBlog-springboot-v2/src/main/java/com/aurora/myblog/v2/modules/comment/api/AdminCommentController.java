package com.aurora.myblog.v2.modules.comment.api;

import com.aurora.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.aurora.myblog.v2.common.auth.CurrentUser;
import com.aurora.myblog.v2.common.web.ApiResponse;
import com.aurora.myblog.v2.common.web.PageResponse;
import com.aurora.myblog.v2.modules.comment.application.AdminCommentCommandService;
import com.aurora.myblog.v2.modules.comment.application.AdminCommentQueryService;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentDeletionCommand;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentItem;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentModerationCommand;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentQuery;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentRestoreCommand;
import com.aurora.myblog.v2.modules.comment.domain.CommentType;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/comments")
public class AdminCommentController {

    private final AdminCommentQueryService queryService;
    private final AdminCommentCommandService commandService;

    public AdminCommentController(AdminCommentQueryService queryService,
                                  AdminCommentCommandService commandService) {
        this.queryService = queryService;
        this.commandService = commandService;
    }

    @GetMapping
    ApiResponse<PageResponse<AdminCommentResponse>> list(@RequestParam(required = false) Integer type,
                                                         @RequestParam(required = false) Integer topicId,
                                                         @RequestParam(required = false) Boolean reviewed,
                                                         @RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) Boolean deleted,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size) {
        CommentType commentType = type == null ? null : CommentType.fromCode(type);
        PageResponse<AdminCommentItem> result = queryService.list(new AdminCommentQuery(
                commentType,
                topicId,
                reviewed,
                keyword,
                deleted,
                page,
                size));
        return ApiResponse.ok(new PageResponse<>(
                result.records().stream().map(AdminCommentResponse::from).toList(),
                result.total(),
                result.page(),
                result.size()));
    }

    @GetMapping("/{id}")
    ApiResponse<AdminCommentDetailResponse> detail(@PathVariable int id) {
        return ApiResponse.ok(AdminCommentDetailResponse.from(queryService.detail(id)));
    }

    @PutMapping("/review")
    ApiResponse<AdminCommentCommandService.Result> review(@CurrentUser AuthenticatedPrincipal currentUser,
                                                          @Valid @RequestBody AdminCommentReviewRequest request) {
        return ApiResponse.ok(commandService.review(new AdminCommentModerationCommand(
                request.ids(),
                request.reviewed(),
                Integer.parseInt(currentUser.id()))));
    }

    @DeleteMapping
    ApiResponse<AdminCommentCommandService.Result> delete(@CurrentUser AuthenticatedPrincipal currentUser,
                                                          @Valid @RequestBody AdminCommentDeleteRequest request) {
        return ApiResponse.ok(commandService.delete(new AdminCommentDeletionCommand(
                request.ids(),
                Integer.parseInt(currentUser.id()))));
    }

    @PutMapping("/restore")
    ApiResponse<AdminCommentCommandService.Result> restore(@CurrentUser AuthenticatedPrincipal currentUser,
                                                           @Valid @RequestBody AdminCommentRestoreRequest request) {
        return ApiResponse.ok(commandService.restore(new AdminCommentRestoreCommand(
                request.ids(),
                Integer.parseInt(currentUser.id()))));
    }
}
