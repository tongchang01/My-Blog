package com.aurora.myblog.v2.modules.comment.api;

import com.aurora.myblog.v2.common.web.ApiResponse;
import com.aurora.myblog.v2.common.web.PageResponse;
import com.aurora.myblog.v2.modules.comment.application.CommentQueryService;
import com.aurora.myblog.v2.modules.comment.domain.CommentThread;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CommentController {

    private final CommentQueryService commentQueryService;

    public CommentController(CommentQueryService commentQueryService) {
        this.commentQueryService = commentQueryService;
    }

    @GetMapping("/api/comments")
    public ApiResponse<PageResponse<CommentResponse>> listComments(
            @RequestParam Integer type,
            @RequestParam(required = false) Integer topicId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(mapPage(commentQueryService.listComments(type, topicId, page, size)));
    }

    @GetMapping("/api/comments/{commentId}/replies")
    public ApiResponse<List<CommentReplyResponse>> listReplies(@PathVariable int commentId) {
        return ApiResponse.ok(commentQueryService.listRepliesByCommentId(commentId).stream()
                .map(CommentReplyResponse::from)
                .toList());
    }

    @GetMapping("/api/comments/top")
    public ApiResponse<List<CommentResponse>> listTopComments() {
        return ApiResponse.ok(commentQueryService.listTopComments().stream()
                .map(CommentResponse::from)
                .toList());
    }

    private PageResponse<CommentResponse> mapPage(PageResponse<CommentThread> page) {
        return new PageResponse<>(
                page.records().stream().map(CommentResponse::from).toList(),
                page.total(),
                page.page(),
                page.size());
    }
}
