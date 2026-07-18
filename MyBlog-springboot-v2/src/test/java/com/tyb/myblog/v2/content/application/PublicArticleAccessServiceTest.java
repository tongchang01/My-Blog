package com.tyb.myblog.v2.content.application;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.article.ArticleAccessRateLimitService;
import com.tyb.myblog.v2.content.application.article.ArticleAccessTokenResult;
import com.tyb.myblog.v2.content.application.article.PublicArticleAccessService;
import com.tyb.myblog.v2.content.domain.article.ArticleAccessTokenRecord;
import com.tyb.myblog.v2.content.domain.article.ArticleAccessTokenRepository;
import com.tyb.myblog.v2.content.domain.article.ArticlePasswordHasher;
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import com.tyb.myblog.v2.content.domain.article.PublicArticleAccessMetadata;
import com.tyb.myblog.v2.content.domain.article.PublicArticleQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicArticleAccessServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 18, 12, 0);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-18T03:00:00Z"), ZoneId.of("Asia/Tokyo"));

    @Mock
    private PublicArticleQueryRepository articleRepository;
    @Mock
    private ArticleAccessTokenRepository tokenRepository;
    @Mock
    private ArticlePasswordHasher passwordHasher;
    @Mock
    private ArticleAccessRateLimitService rateLimitService;

    private PublicArticleAccessService service;

    @BeforeEach
    void setUp() {
        service = new PublicArticleAccessService(
                articleRepository,
                tokenRepository,
                passwordHasher,
                rateLimitService,
                CLOCK);
    }

    @Test
    void issuesOpaqueTokenForCorrectPasswordAndStoresOnlyItsHash() {
        when(articleRepository.findPublicAccessMetadata(100L, NOW))
                .thenReturn(Optional.of(passwordArticle(100L)));
        when(passwordHasher.matches("open-sesame", "bcrypt-hash"))
                .thenReturn(true);

        ArticleAccessTokenResult result = service.unlock(
                100L, "open-sesame", "127.0.0.1");

        assertThat(result.token()).hasSize(43);
        assertThat(result.expiresAt()).isEqualTo(NOW.plusHours(24));
        ArgumentCaptor<ArticleAccessTokenRecord> token = ArgumentCaptor.forClass(
                ArticleAccessTokenRecord.class);
        verify(tokenRepository).save(token.capture());
        assertThat(token.getValue().articleId()).isEqualTo(100L);
        assertThat(token.getValue().tokenHash()).hasSize(64).isNotEqualTo(result.token());
        assertThat(token.getValue().expiresAt()).isEqualTo(NOW.plusHours(24));
        verify(rateLimitService).checkAndRecord("127.0.0.1", 100L);
    }

    @Test
    void rejectsIncorrectPasswordAndInvalidOrRevokedAccessTokens() {
        when(articleRepository.findPublicAccessMetadata(100L, NOW))
                .thenReturn(Optional.of(passwordArticle(100L)));
        when(passwordHasher.matches("wrong", "bcrypt-hash")).thenReturn(false);

        assertError(() -> service.unlock(100L, "wrong", "127.0.0.1"),
                ApiErrorCode.FORBIDDEN);
        verify(tokenRepository, org.mockito.Mockito.never()).save(any());

        when(tokenRepository.existsActive(eq(100L), any(), eq(NOW))).thenReturn(false);
        assertError(() -> service.requireAccess(passwordArticle(100L), "expired", NOW),
                ApiErrorCode.FORBIDDEN);
    }

    @Test
    void allowsPublishedArticlesWithoutTokenAndRevokesAllForArticle() {
        service.requireAccess(new PublicArticleAccessMetadata(
                101L, ArticleStatus.PUBLISHED, null), null, NOW);

        service.revokeAll(100L);

        verify(tokenRepository).revokeAllByArticleId(100L);
    }

    private static PublicArticleAccessMetadata passwordArticle(long id) {
        return new PublicArticleAccessMetadata(id, ArticleStatus.PASSWORD, "bcrypt-hash");
    }

    private static void assertError(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable callable,
            ApiErrorCode code) {
        assertThatThrownBy(callable)
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.code()).isEqualTo(code));
    }
}
