package com.tyb.myblog.v2.comment.web;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.auth.CurrentUser;
import com.tyb.myblog.v2.common.web.ApiResponse;
import com.tyb.myblog.v2.common.web.ClientIpResolver;
import com.tyb.myblog.v2.common.web.PageResponse;
import com.tyb.myblog.v2.common.web.UserAgentResolver;
import com.tyb.myblog.v2.comment.application.CommentCommandService;
import com.tyb.myblog.v2.comment.application.CommentQueryService;
import com.tyb.myblog.v2.comment.domain.CommentThread;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
/**
 * 前台评论接口。
 *
 * <p>负责评论展示、回复展示、热门评论和评论提交。
 * 评论提交时会记录客户端 IP 和 User-Agent，供后续审核和安全审计使用。</p>
 */
public class CommentController {

    private final CommentQueryService commentQueryService;
    private final CommentCommandService commentCommandService;

    public CommentController(CommentQueryService commentQueryService,
                             CommentCommandService commentCommandService) {
        this.commentQueryService = commentQueryService;
        this.commentCommandService = commentCommandService;
    }

    /**
     * 查询前台评论分页列表。
     */
    @GetMapping("/api/comments")
    public ApiResponse<PageResponse<CommentResponse>> listComments(
            @RequestParam Integer type,
            @RequestParam(required = false) Integer topicId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(mapPage(commentQueryService.listComments(type, topicId, page, size)));
    }

    /**
     * 查询指定评论下的回复列表。
     */
    @GetMapping("/api/comments/{commentId}/replies")
    public ApiResponse<List<CommentReplyResponse>> listReplies(@PathVariable int commentId) {
        return ApiResponse.ok(commentQueryService.listRepliesByCommentId(commentId).stream()
                .map(CommentReplyResponse::from)
                .toList());
    }

    /**
     * 查询首页热门评论。
     */
    @GetMapping("/api/comments/top")
    public ApiResponse<List<CommentResponse>> listTopComments() {
        return ApiResponse.ok(commentQueryService.listTopComments().stream()
                .map(CommentResponse::from)
                .toList());
    }

    /**
     * 提交评论或回复。
     */
    @PostMapping("/api/comments")
    public ApiResponse<CommentCommandService.CommentCreateResult> saveComment(
            @CurrentUser AuthenticatedPrincipal currentUser,
            @Valid @RequestBody CommentCreateRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(commentCommandService.createComment(
                currentUser.id(),
                request.type(),
                request.topicId(),
                request.parentId(),
                request.replyUserId(),
                request.content(),
                ClientIpResolver.resolve(servletRequest),
                UserAgentResolver.resolve(servletRequest)));
    }

    /**
     * 将评论线程分页转换为前台响应分页。
     */
    private PageResponse<CommentResponse> mapPage(PageResponse<CommentThread> page) {
        return new PageResponse<>(
                page.records().stream().map(CommentResponse::from).toList(),
                page.total(),
                page.page(),
                page.size());
    }
}
