package com.tyb.myblog.v2.comment.application;

import com.tyb.myblog.v2.comment.domain.CommentPage;
import com.tyb.myblog.v2.comment.domain.CommentPageItem;
import com.tyb.myblog.v2.comment.domain.CommentQueryRepository;
import com.tyb.myblog.v2.comment.domain.CommentTarget;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.article.ArticleCommentPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final CommentQueryRepository repository;
    private final ArticleCommentPolicyService articlePolicyService;

    public CommentPageResult articleComments(
            long articleId,
            int page,
            int size) {
        validatePage(page, size);
        articlePolicyService.requirePublicCommentable(articleId);
        return toResult(repository.page(
                CommentTarget.article(articleId), page, size));
    }

    public CommentPageResult articleComments(
            long articleId,
            int page,
            int size,
            String articleAccessToken) {
        validatePage(page, size);
        articlePolicyService.requirePublicCommentable(articleId, articleAccessToken);
        return toResult(repository.page(
                CommentTarget.article(articleId), page, size));
    }

    public CommentPageResult guestbookComments(int page, int size) {
        validatePage(page, size);
        return toResult(repository.page(
                CommentTarget.guestbook(), page, size));
    }

    private static void validatePage(int page, int size) {
        if (page < 1) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "页码必须大于 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "每页数量必须在 1 到 100 之间");
        }
    }

    private static CommentPageResult toResult(CommentPage page) {
        return new CommentPageResult(
                page.records().stream()
                        .map(CommentQueryService::toItem)
                        .toList(),
                page.total(),
                page.page(),
                page.size());
    }

    private static CommentPageResult.Item toItem(CommentPageItem item) {
        return new CommentPageResult.Item(
                item.id(),
                item.parentId(),
                item.replyToCommentId(),
                item.replyToNickname(),
                item.authorNickname(),
                item.authorSite(),
                item.contentHtml(),
                item.createdAt(),
                item.replies().stream()
                        .map(CommentQueryService::toItem)
                        .toList());
    }
}
