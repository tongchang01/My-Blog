package com.tyb.myblog.v2.content.application;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.article.AdminArticleDetailResult;
import com.tyb.myblog.v2.content.application.article.AdminArticleQuery;
import com.tyb.myblog.v2.content.application.article.ArticleQueryService;
import com.tyb.myblog.v2.content.domain.article.AdminArticleCriteria;
import com.tyb.myblog.v2.content.domain.article.AdminArticleDetail;
import com.tyb.myblog.v2.content.domain.article.AdminArticlePage;
import com.tyb.myblog.v2.content.domain.article.AdminArticlePageItem;
import com.tyb.myblog.v2.content.domain.article.AdminArticleQueryRepository;
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import com.tyb.myblog.v2.system.application.attachment.AttachmentReferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminArticleQueryServiceTest {

    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 6, 16, 12, 0);

    @Mock
    private AdminArticleQueryRepository repository;

    @Mock
    private AttachmentReferenceService attachmentService;

    private ArticleQueryService service;

    @BeforeEach
    void setUp() {
        service = new ArticleQueryService(
                repository,
                new ContentAuthorization(),
                attachmentService);
    }

    @Test
    void allowsAdminAndDemoToReadPagesWithResolvedCoverUrls() {
        AdminArticleQuery query = query(1, 20);
        when(repository.findActivePage(criteria(query)))
                .thenReturn(new AdminArticlePage(
                        List.of(pageItem(10L, 300L)),
                        1,
                        1,
                        20));
        when(attachmentService.resolvePublicUrls(Set.of(300L)))
                .thenReturn(Map.of(300L, "https://cdn.example.com/c.png"));

        assertThat(service.adminPage(
                principal("ADMIN"), query).records())
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.id()).isEqualTo(10L);
                    assertThat(item.coverUrl())
                            .isEqualTo("https://cdn.example.com/c.png");
                });
        assertThat(service.adminPage(
                principal("DEMO"), query).total()).isEqualTo(1);
    }

    @Test
    void returnsDetailWithoutPasswordHashAndWithCoverUrl() {
        when(repository.findActiveDetail(10L))
                .thenReturn(Optional.of(detail(
                        10L,
                        300L,
                        ArticleStatus.PASSWORD)));
        when(attachmentService.resolvePublicUrls(Set.of(300L)))
                .thenReturn(Map.of(300L, "https://cdn.example.com/c.png"));

        AdminArticleDetailResult result =
                service.adminDetail(principal("DEMO"), 10L);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.coverUrl())
                .isEqualTo("https://cdn.example.com/c.png");
        assertThat(result.tagIds()).containsExactly(20L, 30L);
    }

    @Test
    void adminCanReadBodyForEveryArticleStatus() {
        for (ArticleStatus status : ArticleStatus.values()) {
            long id = 100L + status.databaseValue();
            when(repository.findActiveDetail(id))
                    .thenReturn(Optional.of(detail(id, null, status)));

            assertThat(service.adminDetail(
                    principal("ADMIN"), id).body())
                    .as(status.name())
                    .isEqualTo("正文");
        }
    }

    @Test
    void demoCanReadPublishedBody() {
        when(repository.findActiveDetail(10L))
                .thenReturn(Optional.of(detail(
                        10L,
                        null,
                        ArticleStatus.PUBLISHED)));

        assertThat(service.adminDetail(
                principal("DEMO"), 10L).body())
                .isEqualTo("正文");
    }

    @ParameterizedTest
    @EnumSource(
            value = ArticleStatus.class,
            names = {"DRAFT", "PRIVATE", "PASSWORD", "SCHEDULED"})
    void demoCannotReadNonPublishedBody(ArticleStatus status) {
        when(repository.findActiveDetail(10L))
                .thenReturn(Optional.of(detail(10L, null, status)));

        assertThat(service.adminDetail(
                principal("DEMO"), 10L).body())
                .isNull();
    }

    @Test
    void validatesPageIdentityAndTimeRange() {
        assertError(
                () -> service.adminPage(null, query(1, 20)),
                ApiErrorCode.INVALID_TOKEN);
        assertError(
                () -> service.adminPage(
                        principal("GUEST"), query(1, 20)),
                ApiErrorCode.FORBIDDEN);
        assertError(
                () -> service.adminPage(
                        principal("ADMIN"), query(0, 20)),
                ApiErrorCode.VALIDATION_ERROR);
        assertError(
                () -> service.adminPage(
                        principal("ADMIN"), query(1, 101)),
                ApiErrorCode.VALIDATION_ERROR);
        assertError(
                () -> service.adminPage(
                        principal("ADMIN"),
                        new AdminArticleQuery(
                                1,
                                20,
                                null,
                                null,
                                null,
                                null,
                                NOW,
                                NOW.minusDays(1),
                                null,
                                null)),
                ApiErrorCode.VALIDATION_ERROR);
    }

    @Test
    void mapsMissingDetailToNotFound() {
        when(repository.findActiveDetail(404L))
                .thenReturn(Optional.empty());

        assertError(
                () -> service.adminDetail(principal("ADMIN"), 404L),
                ApiErrorCode.NOT_FOUND);
    }

    private AdminArticleQuery query(int page, int size) {
        return new AdminArticleQuery(
                page,
                size,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private AdminArticleCriteria criteria(AdminArticleQuery query) {
        return new AdminArticleCriteria(
                query.page(),
                query.size(),
                query.status(),
                query.categoryId(),
                query.tagId(),
                query.titleKeyword(),
                query.createdFrom(),
                query.createdTo(),
                query.publishFrom(),
                query.publishTo());
    }

    private AdminArticlePageItem pageItem(long id, Long coverId) {
        return new AdminArticlePageItem(
                id,
                "标题",
                null,
                null,
                "摘要",
                null,
                null,
                10L,
                "分类",
                "article-" + id,
                ArticleStatus.PUBLISHED,
                NOW.minusDays(1),
                coverId,
                null,
                2,
                List.of(20L),
                NOW.minusDays(2),
                1001L,
                NOW.minusHours(1),
                1001L);
    }

    private AdminArticleDetail detail(
            long id,
            Long coverId,
            ArticleStatus status) {
        return new AdminArticleDetail(
                id,
                "标题",
                null,
                null,
                "摘要",
                null,
                null,
                "正文",
                10L,
                "分类",
                1001L,
                "article-" + id,
                status,
                NOW.minusDays(1),
                coverId,
                null,
                2,
                List.of(20L, 30L),
                NOW.minusDays(2),
                1001L,
                NOW.minusHours(1),
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
