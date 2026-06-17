package com.tyb.myblog.v2.content.application;

import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.article.ArticleCommentCountService;
import com.tyb.myblog.v2.content.application.article.ArticleCommentGateway;
import com.tyb.myblog.v2.content.application.article.ArticleCommentPolicy;
import com.tyb.myblog.v2.content.application.article.ArticleCommentPolicySnapshot;
import com.tyb.myblog.v2.content.application.article.ArticleCommentPolicyService;
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleCommentPolicyServiceTest {

    private final ArticleCommentGateway gateway =
            mock(ArticleCommentGateway.class);
    private final ArticleCommentPolicyService policyService =
            new ArticleCommentPolicyService(gateway);
    private final ArticleCommentCountService countService =
            new ArticleCommentCountService(gateway);

    @Test
    void publishedArticleAllowsComments() {
        when(gateway.findCommentPolicy(100L))
                .thenReturn(Optional.of(row(ArticleStatus.PUBLISHED, 3)));

        ArticleCommentPolicy policy =
                policyService.requirePublicCommentable(100L);

        assertThat(policy.articleId()).isEqualTo(100L);
        assertThat(policy.commentCount()).isEqualTo(3);
    }

    @Test
    void passwordArticleIsForbiddenAndNonPublicIsNotFound() {
        when(gateway.findCommentPolicy(101L))
                .thenReturn(Optional.of(row(ArticleStatus.PASSWORD, 0)));
        when(gateway.findCommentPolicy(102L))
                .thenReturn(Optional.of(row(ArticleStatus.DRAFT, 0)));

        assertThatThrownBy(() -> policyService.requirePublicCommentable(101L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("无权");
        assertThatThrownBy(() -> policyService.requirePublicCommentable(102L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    void commentCountCannotBecomeNegative() {
        when(gateway.incrementCommentCount(100L, -1)).thenReturn(false);

        assertThatThrownBy(() -> countService.increment(100L, -1))
                .isInstanceOf(ApiException.class);

        verify(gateway).incrementCommentCount(100L, -1);
    }

    private static ArticleCommentPolicySnapshot row(
            ArticleStatus status,
            int commentCount) {
        return new ArticleCommentPolicySnapshot(100L, status, commentCount);
    }
}
