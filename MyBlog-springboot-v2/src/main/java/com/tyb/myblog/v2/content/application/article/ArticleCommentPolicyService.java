package com.tyb.myblog.v2.content.application.article;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArticleCommentPolicyService {

    private final ArticleCommentGateway gateway;
    private final PublicArticleAccessService accessService;

    public ArticleCommentPolicy requirePublicCommentable(long articleId) {
        return requirePublicCommentable(articleId, null);
    }

    public ArticleCommentPolicy requirePublicCommentable(
            long articleId,
            String articleAccessToken) {
        ArticleCommentPolicySnapshot snapshot = gateway.findCommentPolicy(articleId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
        ArticleStatus status = snapshot.status();
        if (status == ArticleStatus.PASSWORD) {
            accessService.requirePasswordAccess(articleId, articleAccessToken);
        }
        if (status != ArticleStatus.PUBLISHED && status != ArticleStatus.PASSWORD) {
            throw new ApiException(ApiErrorCode.NOT_FOUND);
        }
        return new ArticleCommentPolicy(
                snapshot.articleId(),
                snapshot.commentCount());
    }
}
