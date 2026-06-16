package com.tyb.myblog.v2.content.application;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.article.ArticleDeleteService;
import com.tyb.myblog.v2.content.application.article.ArticleReferenceValidator;
import com.tyb.myblog.v2.content.application.article.ArticleRestoreService;
import com.tyb.myblog.v2.content.application.article.DeletedArticleQueryService;
import com.tyb.myblog.v2.content.domain.article.AdminArticleQueryRepository;
import com.tyb.myblog.v2.content.domain.article.Article;
import com.tyb.myblog.v2.content.domain.article.ArticleRepository;
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import com.tyb.myblog.v2.content.domain.article.DeletedArticlePage;
import com.tyb.myblog.v2.content.domain.article.DeletedArticlePageItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleDeleteRestoreServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-16T03:00:00Z"),
            ZoneId.of("Asia/Tokyo"));
    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 6, 16, 12, 0);

    @Mock
    private ArticleRepository repository;

    @Mock
    private AdminArticleQueryRepository queryRepository;

    @Mock
    private ArticleReferenceValidator referenceValidator;

    private ArticleDeleteService deleteService;
    private ArticleRestoreService restoreService;
    private DeletedArticleQueryService queryService;

    @BeforeEach
    void setUp() {
        ContentAuthorization authorization = new ContentAuthorization();
        deleteService = new ArticleDeleteService(
                repository, authorization, CLOCK);
        restoreService = new ArticleRestoreService(
                repository, referenceValidator, authorization, CLOCK);
        queryService = new DeletedArticleQueryService(
                queryRepository, authorization);
    }

    @Test
    void softDeletesAsAdminAndRejectsDemo() {
        when(repository.softDelete(10L, NOW, 1001L))
                .thenReturn(true);

        deleteService.delete(principal("ADMIN"), 10L);

        verify(repository).softDelete(10L, NOW, 1001L);
        assertError(
                () -> deleteService.delete(principal("DEMO"), 10L),
                ApiErrorCode.FORBIDDEN);
    }

    @Test
    void restoresAfterReferenceValidationAndConvertsMissingReferenceToConflict() {
        Article deleted = article(10L, List.of(20L));
        when(repository.findDeletedByIdForUpdate(10L))
                .thenReturn(Optional.of(deleted));
        when(repository.restore(10L, NOW, 1001L))
                .thenReturn(true);

        assertThat(restoreService.restore(principal("ADMIN"), 10L).id())
                .isEqualTo(10L);
        verify(referenceValidator).lockAndValidate(
                ArticleStatus.PUBLISHED, 10L, List.of(20L), 300L);
        verify(repository).restore(10L, NOW, 1001L);

        when(repository.findDeletedByIdForUpdate(11L))
                .thenReturn(Optional.of(article(11L, List.of(21L))));
        org.mockito.Mockito.doThrow(new ApiException(ApiErrorCode.NOT_FOUND))
                .when(referenceValidator)
                .lockAndValidate(
                        ArticleStatus.PUBLISHED, 10L, List.of(21L), 300L);

        assertError(
                () -> restoreService.restore(principal("ADMIN"), 11L),
                ApiErrorCode.CONFLICT);
        verify(repository, never()).restore(11L, NOW, 1001L);
    }

    @Test
    void pagesRecycleBinForAdminAndDemo() {
        when(queryRepository.findDeletedPage(1, 20))
                .thenReturn(new DeletedArticlePage(
                        List.of(new DeletedArticlePageItem(
                                10L,
                                "标题",
                                null,
                                null,
                                ArticleStatus.PUBLISHED,
                                20L,
                                NOW,
                                1001L)),
                        1,
                        1,
                        20));

        assertThat(queryService.page(principal("DEMO"), 1, 20).records())
                .singleElement()
                .satisfies(item -> assertThat(item.id()).isEqualTo(10L));
    }

    private Article article(long id, List<Long> tags) {
        return Article.reconstitute(
                id,
                "标题",
                null,
                null,
                "摘要",
                null,
                null,
                "正文",
                10L,
                1001L,
                "article-" + id,
                ArticleStatus.PUBLISHED,
                null,
                NOW.minusDays(1),
                300L,
                0,
                tags,
                NOW.minusDays(2),
                1001L,
                NOW.minusDays(1),
                1001L,
                true,
                NOW,
                1001L);
    }

    private AuthenticatedPrincipal principal(String role) {
        return new AuthenticatedPrincipal(
                "1001", role.toLowerCase(), List.of(role));
    }

    private void assertError(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable callable,
            ApiErrorCode code) {
        assertThatThrownBy(callable)
                .isInstanceOfSatisfying(
                        ApiException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(code));
    }
}
