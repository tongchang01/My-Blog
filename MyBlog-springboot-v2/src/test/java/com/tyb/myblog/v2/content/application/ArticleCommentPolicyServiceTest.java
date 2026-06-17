package com.tyb.myblog.v2.content.application;

import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.article.ArticleCommentCountService;
import com.tyb.myblog.v2.content.application.article.ArticleCommentPolicy;
import com.tyb.myblog.v2.content.application.article.ArticleCommentPolicyService;
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import com.tyb.myblog.v2.content.infrastructure.persistence.mapper.ArticleMapper;
import com.tyb.myblog.v2.content.infrastructure.persistence.projection.ArticleCommentPolicyRow;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleCommentPolicyServiceTest {

    private final ArticleMapper mapper = mock(ArticleMapper.class);
    private final ArticleCommentPolicyService policyService =
            new ArticleCommentPolicyService(mapper);
    private final ArticleCommentCountService countService =
            new ArticleCommentCountService(mapper);

    @Test
    void publishedArticleAllowsComments() {
        when(mapper.selectCommentPolicy(100L))
                .thenReturn(row(ArticleStatus.PUBLISHED, 3));

        ArticleCommentPolicy policy =
                policyService.requirePublicCommentable(100L);

        assertThat(policy.articleId()).isEqualTo(100L);
        assertThat(policy.commentCount()).isEqualTo(3);
    }

    @Test
    void passwordArticleIsForbiddenAndNonPublicIsNotFound() {
        when(mapper.selectCommentPolicy(101L))
                .thenReturn(row(ArticleStatus.PASSWORD, 0));
        when(mapper.selectCommentPolicy(102L))
                .thenReturn(row(ArticleStatus.DRAFT, 0));

        assertThatThrownBy(() -> policyService.requirePublicCommentable(101L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("无权");
        assertThatThrownBy(() -> policyService.requirePublicCommentable(102L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    void commentCountCannotBecomeNegative() {
        when(mapper.incrementCommentCount(100L, -1)).thenReturn(0);

        assertThatThrownBy(() -> countService.increment(100L, -1))
                .isInstanceOf(ApiException.class);

        verify(mapper).incrementCommentCount(100L, -1);
    }

    private static ArticleCommentPolicyRow row(
            ArticleStatus status,
            int commentCount) {
        ArticleCommentPolicyRow row = new ArticleCommentPolicyRow();
        row.setId(100L);
        row.setStatus(status.databaseValue());
        row.setCommentCount(commentCount);
        return row;
    }
}
