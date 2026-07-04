package com.tyb.myblog.v2.content.application;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.article.PublicArchivePageResult;
import com.tyb.myblog.v2.content.application.article.PublicArticleDetailResult;
import com.tyb.myblog.v2.content.application.article.PublicArticleHomeResult;
import com.tyb.myblog.v2.content.application.article.PublicArticleQuery;
import com.tyb.myblog.v2.content.application.article.PublicArticleQueryService;
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import com.tyb.myblog.v2.content.domain.article.ArticleTagView;
import com.tyb.myblog.v2.content.domain.article.PublicArticleAccessMetadata;
import com.tyb.myblog.v2.content.domain.article.PublicArticleDetail;
import com.tyb.myblog.v2.content.domain.article.PublicArticleHome;
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
    void returnsPublicPageWhenArticleHasNoCover() {
        PublicArticleQuery query = new PublicArticleQuery(
                1, 20, "zh", null, null, null, null);
        PublicArticlePageItem item = pageItem(
                100L,
                ArticleStatus.PUBLISHED,
                (Long) null);
        when(repository.findPublicPage(query.toCriteria(NOW)))
                .thenReturn(new PublicArticlePage(
                        List.of(item),
                        1,
                        1,
                        20));
        when(attachmentService.resolvePublicUrls(Set.of()))
                .thenReturn(Map.of());

        assertThat(service.page(query).records())
                .singleElement()
                .extracting("coverUrl")
                .isNull();
    }

    @Test
    void returnsArchiveTimelineGroupedByCurrentPagePublishMonth() {
        PublicArticleQuery query = new PublicArticleQuery(
                1, 3, "en", null, null, null, null);
        PublicArticlePageItem previousMonth =
                pageItem(101L, ArticleStatus.PUBLISHED, NOW.minusMonths(1));
        when(repository.findPublicPage(query.toCriteria(NOW)))
                .thenReturn(new PublicArticlePage(
                        List.of(
                                pageItem(103L, ArticleStatus.PUBLISHED, NOW.minusDays(1)),
                                pageItem(102L, ArticleStatus.PASSWORD, NOW.minusDays(2)),
                                previousMonth),
                        8,
                        1,
                        3));

        PublicArchivePageResult result = service.archives(query);

        assertThat(result.total()).isEqualTo(8);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(3);
        assertThat(result.records()).hasSize(2);
        assertThat(result.records().get(0).yearMonth()).isEqualTo("2026-06");
        assertThat(result.records().get(0).articles())
                .extracting("id")
                .containsExactly(103L, 102L);
        assertThat(result.records().get(1).yearMonth()).isEqualTo("2026-05");
        assertThat(result.records().get(1).articles())
                .singleElement()
                .satisfies(article -> {
                    assertThat(article.title()).isEqualTo("English Title");
                    assertThat(article.summary()).isEqualTo(previousMonth.summaryZh());
                    assertThat(article.slug()).isEqualTo("article-101");
                    assertThat(article.publishedAt()).isEqualTo(NOW.minusMonths(1));
                });
        verify(attachmentService, never()).resolvePublicUrls(Set.of());
    }

    @Test
    void returnsHomepageArticlesWithPinnedFeaturedAndOrdinaryGroups() {
        PublicArticlePageItem pinned = pageItem(
                100L,
                ArticleStatus.PUBLISHED,
                300L);
        PublicArticlePageItem featured = pageItem(
                101L,
                ArticleStatus.PUBLISHED,
                (Long) null);
        PublicArticlePageItem ordinary = pageItem(
                102L,
                ArticleStatus.PUBLISHED,
                301L);
        when(repository.findPublicHome(NOW, 10))
                .thenReturn(new PublicArticleHome(
                        pinned,
                        List.of(featured),
                        List.of(ordinary)));
        when(attachmentService.resolvePublicUrls(Set.of(300L, 301L)))
                .thenReturn(Map.of(
                        300L, "https://cdn.example.com/pinned.png",
                        301L, "https://cdn.example.com/article.png"));

        PublicArticleHomeResult result = service.home("zh", 10);

        assertThat(result.pinnedArticle()).isNotNull();
        assertThat(result.pinnedArticle().coverUrl())
                .isEqualTo("https://cdn.example.com/pinned.png");
        assertThat(result.featuredArticles())
                .singleElement()
                .extracting("id")
                .isEqualTo(101L);
        assertThat(result.articles())
                .singleElement()
                .extracting("coverUrl")
                .isEqualTo("https://cdn.example.com/article.png");
    }

    @Test
    void validatesHomepageSize() {
        assertError(
                () -> service.home("zh", 0),
                ApiErrorCode.VALIDATION_ERROR);
        assertError(
                () -> service.home("zh", 51),
                ApiErrorCode.VALIDATION_ERROR);
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
        return pageItem(id, status, 300L);
    }

    private PublicArticlePageItem pageItem(
            long id,
            ArticleStatus status,
            Long coverAttachmentId) {
        return pageItem(id, status, coverAttachmentId, NOW.minusDays(1));
    }

    private PublicArticlePageItem pageItem(
            long id,
            ArticleStatus status,
            LocalDateTime publishAt) {
        return pageItem(id, status, 300L, publishAt);
    }

    private PublicArticlePageItem pageItem(
            long id,
            ArticleStatus status,
            Long coverAttachmentId,
            LocalDateTime publishAt) {
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
                publishAt,
                coverAttachmentId,
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
