package com.tyb.myblog.v2.content.application;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.article.PublicArticleDetailResult;
import com.tyb.myblog.v2.content.application.article.PublicArticleQuery;
import com.tyb.myblog.v2.content.application.article.PublicArticleQueryService;
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import com.tyb.myblog.v2.content.domain.article.ArticleTagView;
import com.tyb.myblog.v2.content.domain.article.PublicArticleAccessMetadata;
import com.tyb.myblog.v2.content.domain.article.PublicArticleDetail;
import com.tyb.myblog.v2.content.domain.article.PublicArticlePage;
import com.tyb.myblog.v2.content.domain.article.PublicArticlePageItem;
import com.tyb.myblog.v2.content.domain.article.PublicArticleQueryRepository;
import com.tyb.myblog.v2.system.application.attachment.AttachmentReferenceService;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicArticleQueryServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-16T03:00:00Z"),
            ZoneId.of("Asia/Tokyo"));
    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 6, 16, 12, 0);

    @Mock
    private PublicArticleQueryRepository repository;

    @Mock
    private AttachmentReferenceService attachmentService;

    private PublicArticleQueryService service;

    @BeforeEach
    void setUp() {
        service = new PublicArticleQueryService(
                repository,
                attachmentService,
                CLOCK);
    }

    @Test
    void returnsPublicPageWithLanguageFallbackCoverUrlsAndLockedFlag() {
        PublicArticleQuery query = new PublicArticleQuery(
                1, 20, "ja", 10L, 20L, "Spring", "2026-06");
        when(repository.findPublicPage(query.toCriteria(NOW)))
                .thenReturn(new PublicArticlePage(
                        List.of(pageItem(100L, ArticleStatus.PASSWORD)),
                        1,
                        1,
                        20));
        when(attachmentService.resolvePublicUrls(Set.of(300L)))
                .thenReturn(Map.of(300L, "https://cdn.example.com/c.png"));

        assertThat(service.page(query).records())
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.title()).isEqualTo("中文标题");
                    assertThat(item.summary()).isEqualTo("日本語概要");
                    assertThat(item.categoryName()).isEqualTo("分类");
                    assertThat(item.locked()).isTrue();
                    assertThat(item.coverUrl())
                            .isEqualTo("https://cdn.example.com/c.png");
                    assertThat(item.tags())
                            .extracting("name")
                            .containsExactly("タグ");
                });
    }

    @Test
    void returnsPublishedDetailBodyAndRejectsPasswordDetail() {
        when(repository.findPublicAccessMetadata(100L, NOW))
                .thenReturn(Optional.of(new PublicArticleAccessMetadata(
                        100L,
                        ArticleStatus.PUBLISHED)));
        when(repository.findPublicAccessMetadata(101L, NOW))
                .thenReturn(Optional.of(new PublicArticleAccessMetadata(
                        101L,
                        ArticleStatus.PASSWORD)));
        when(repository.findPublicDetail(100L, NOW))
                .thenReturn(Optional.of(detail(
                        100L, ArticleStatus.PUBLISHED)));

        PublicArticleDetailResult result = service.detail(100L, "en");
        assertThat(result.body()).isEqualTo("正文");
        assertThat(result.locked()).isFalse();

        assertError(
                () -> service.detail(101L, "zh"),
                ApiErrorCode.FORBIDDEN);
        verify(repository, never()).findPublicDetail(101L, NOW);
    }

    @Test
    void rejectsMissingPublicAccessMetadata() {
        when(repository.findPublicAccessMetadata(999L, NOW))
                .thenReturn(Optional.empty());

        assertError(
                () -> service.detail(999L, "zh"),
                ApiErrorCode.NOT_FOUND);
        verify(repository, never()).findPublicDetail(999L, NOW);
    }

    @Test
    void validatesPageSizeArchiveMonthAndIdentity() {
        assertError(
                () -> service.page(new PublicArticleQuery(
                        0, 20, "zh", null, null, null, null)),
                ApiErrorCode.VALIDATION_ERROR);
        assertError(
                () -> service.page(new PublicArticleQuery(
                        1, 101, "zh", null, null, null, null)),
                ApiErrorCode.VALIDATION_ERROR);
        assertError(
                () -> service.page(new PublicArticleQuery(
                        1, 20, "zh", null, null, null, "2026/06")),
                ApiErrorCode.VALIDATION_ERROR);
        assertError(
                () -> service.detail(0L, "zh"),
                ApiErrorCode.VALIDATION_ERROR);
    }

    private PublicArticlePageItem pageItem(
            long id,
            ArticleStatus status) {
        return new PublicArticlePageItem(
                id,
                "中文标题",
                null,
                "English Title",
                "中文概要",
                "日本語概要",
                null,
                10L,
                "分类",
                null,
                "Category",
                "article-" + id,
                status,
                NOW.minusDays(1),
                300L,
                null,
                2,
                List.of(tag()),
                NOW.minusDays(2));
    }

    private PublicArticleDetail detail(long id, ArticleStatus status) {
        return new PublicArticleDetail(
                id,
                "中文标题",
                null,
                "English Title",
                "中文概要",
                null,
                null,
                "正文",
                10L,
                "分类",
                null,
                "Category",
                "article-" + id,
                status,
                NOW.minusDays(1),
                300L,
                null,
                2,
                List.of(tag()),
                NOW.minusDays(2),
                NOW.minusDays(1));
    }

    private ArticleTagView tag() {
        return new ArticleTagView(
                20L,
                "标签",
                "タグ",
                null,
                "tag");
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
