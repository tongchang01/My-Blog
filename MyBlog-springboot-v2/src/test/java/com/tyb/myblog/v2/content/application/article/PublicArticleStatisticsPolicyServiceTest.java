package com.tyb.myblog.v2.content.application.article;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublicArticleStatisticsPolicyServiceTest {

    private final ArticleStatisticsGateway gateway =
            mock(ArticleStatisticsGateway.class);

    private PublicArticleStatisticsPolicyService service;

    @BeforeEach
    void setUp() {
        service = new PublicArticleStatisticsPolicyService(gateway);
    }

    @ParameterizedTest
    @EnumSource(value = ArticleStatus.class,
            names = {"PUBLISHED", "PASSWORD"})
    void allowsPubliclyReachableArticles(ArticleStatus status) {
        when(gateway.findPolicy(100L)).thenReturn(Optional.of(
                new ArticleStatisticsPolicySnapshot(100L, status)));

        assertThat(service.requirePublicTrackable(100L))
                .isEqualTo(100L);
    }

    @ParameterizedTest
    @EnumSource(value = ArticleStatus.class,
            names = {"DRAFT", "PRIVATE", "SCHEDULED"})
    void hidesNonPublicArticles(ArticleStatus status) {
        when(gateway.findPolicy(100L)).thenReturn(Optional.of(
                new ArticleStatisticsPolicySnapshot(100L, status)));

        assertNotFound(() -> service.requirePublicTrackable(100L));
    }

    @Test
    void hidesMissingAndInvalidArticles() {
        when(gateway.findPolicy(999L)).thenReturn(Optional.empty());

        assertNotFound(() -> service.requirePublicTrackable(999L));
        assertNotFound(() -> service.requirePublicTrackable(0L));
    }

    private void assertNotFound(Runnable invocation) {
        assertThatThrownBy(invocation::run)
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).code())
                .isEqualTo(ApiErrorCode.NOT_FOUND);
    }
}
