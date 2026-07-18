package com.tyb.myblog.v2.comment.web;

import com.tyb.myblog.v2.comment.application.CommentCreateService;
import com.tyb.myblog.v2.comment.application.CommentPageResult;
import com.tyb.myblog.v2.comment.application.CommentQueryService;
import com.tyb.myblog.v2.common.web.ApiResponse;
import com.tyb.myblog.v2.common.web.ClientIpResolver;
import com.tyb.myblog.v2.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/articles/{articleId:\\d+}/comments")
@RequiredArgsConstructor
public class PublicArticleCommentController {

    private final CommentQueryService queryService;
    private final CommentCreateService createService;
    private final ClientIpResolver clientIpResolver;

    @Operation(summary = "查询公开文章评论")
    @GetMapping
    public ApiResponse<PageResponse<PublicCommentVO>> page(
            @PathVariable long articleId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(name = "X-Article-Access-Token", required = false)
            String articleAccessToken) {
        return ApiResponse.ok(toPageResponse(
                articleAccessToken == null
                        ? queryService.articleComments(articleId, page, size)
                        : queryService.articleComments(
                                articleId, page, size, articleAccessToken)));
    }

    @Operation(summary = "提交公开文章评论")
    @PostMapping
    public ApiResponse<PublicCommentCreateVO> create(
            @PathVariable long articleId,
            @Valid @org.springframework.web.bind.annotation.RequestBody
            PublicCommentCreateRequest request,
            HttpServletRequest servletRequest,
            @RequestHeader(name = "X-Article-Access-Token", required = false)
            String articleAccessToken) {
        return ApiResponse.ok(PublicCommentCreateVO.from(
                articleAccessToken == null
                        ? createService.createArticleComment(request.toCommand(
                                articleId,
                                clientIpResolver.resolve(servletRequest),
                                servletRequest.getHeader("User-Agent")))
                        : createService.createArticleComment(request.toCommand(
                                articleId,
                                clientIpResolver.resolve(servletRequest),
                                servletRequest.getHeader("User-Agent")),
                                articleAccessToken)));
    }

    private static PageResponse<PublicCommentVO> toPageResponse(
            CommentPageResult result) {
        return new PageResponse<>(
                result.records().stream()
                        .map(PublicCommentVO::from)
                        .toList(),
                result.total(),
                result.page(),
                result.size());
    }
}
