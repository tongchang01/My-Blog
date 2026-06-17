package com.tyb.myblog.v2.comment.web;

import com.tyb.myblog.v2.comment.application.CommentCreateService;
import com.tyb.myblog.v2.comment.application.CommentPageResult;
import com.tyb.myblog.v2.comment.application.CommentQueryService;
import com.tyb.myblog.v2.common.web.ApiResponse;
import com.tyb.myblog.v2.common.web.ClientIpResolver;
import com.tyb.myblog.v2.common.web.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/articles/{articleId:\\d+}/comments")
@RequiredArgsConstructor
public class PublicArticleCommentController {

    private final CommentQueryService queryService;
    private final CommentCreateService createService;
    private final ClientIpResolver clientIpResolver;

    @GetMapping
    public ApiResponse<PageResponse<PublicCommentVO>> page(
            @PathVariable long articleId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(toPageResponse(
                queryService.articleComments(articleId, page, size)));
    }

    @PostMapping
    public ApiResponse<PublicCommentCreateVO> create(
            @PathVariable long articleId,
            @org.springframework.web.bind.annotation.RequestBody
            PublicCommentCreateRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(PublicCommentCreateVO.from(
                createService.createArticleComment(request.toCommand(
                        articleId,
                        clientIpResolver.resolve(servletRequest),
                        servletRequest.getHeader("User-Agent")))));
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
