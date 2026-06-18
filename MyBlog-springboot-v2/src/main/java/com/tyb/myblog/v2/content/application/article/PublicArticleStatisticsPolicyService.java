package com.tyb.myblog.v2.content.application.article;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 公开页面访问打点的文章可见性策略。
 */
@Service
@RequiredArgsConstructor
public class PublicArticleStatisticsPolicyService {

    private final ArticleStatisticsGateway gateway;

    public long requirePublicTrackable(long articleId) {
        if (articleId <= 0) {
            throw new ApiException(ApiErrorCode.NOT_FOUND);
        }
        ArticleStatisticsPolicySnapshot snapshot = gateway
                .findPolicy(articleId)
                .orElseThrow(() -> new ApiException(
                        ApiErrorCode.NOT_FOUND));
        if (snapshot.status() != ArticleStatus.PUBLISHED
                && snapshot.status() != ArticleStatus.PASSWORD) {
            throw new ApiException(ApiErrorCode.NOT_FOUND);
        }
        return snapshot.articleId();
    }
}
