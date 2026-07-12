package com.tyb.myblog.v2.content.application;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.article.ArticleCreateService;
import com.tyb.myblog.v2.content.application.article.ArticleReferenceValidator;
import com.tyb.myblog.v2.content.application.article.ArticleResult;
import com.tyb.myblog.v2.content.application.article.ArticleUpdateService;
import com.tyb.myblog.v2.content.application.article.CreateArticleCommand;
import com.tyb.myblog.v2.content.application.article.UpdateArticleCommand;
import com.tyb.myblog.v2.content.domain.article.Article;
import com.tyb.myblog.v2.content.domain.article.ArticlePasswordHasher;
import com.tyb.myblog.v2.content.domain.article.ArticleRepository;
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import com.tyb.myblog.v2.content.domain.article.HomepageSlot;
import com.tyb.myblog.v2.content.domain.article.NewArticle;
import com.tyb.myblog.v2.content.domain.category.Category;
import com.tyb.myblog.v2.content.domain.category.CategoryRepository;
import com.tyb.myblog.v2.content.domain.tag.Tag;
import com.tyb.myblog.v2.content.domain.tag.TagRepository;
import com.tyb.myblog.v2.system.application.attachment.AttachmentReferenceService;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleWriteServiceTest {

    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 6, 15, 12, 0);

    private static final AuthenticatedPrincipal ADMIN =
            principal("1001", "ADMIN");

    private static final AuthenticatedPrincipal DEMO =
            principal("1002", "DEMO");

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private AttachmentReferenceService attachmentService;

    @Mock
    private ArticlePasswordHasher passwordHasher;

    private ArticleCreateService createService;
    private ArticleUpdateService updateService;

    @BeforeEach
    void setUp() {
        ContentAuthorization authorization =
                new ContentAuthorization();
        ArticleReferenceValidator validator =
                new ArticleReferenceValidator(
                        categoryRepository,
                        tagRepository,
                        attachmentService);
        Clock clock = Clock.fixed(
                Instant.parse("2026-06-15T03:00:00Z"),
                ZoneId.of("Asia/Tokyo"));
        createService = new ArticleCreateService(
                articleRepository,
                authorization,
                validator,
                passwordHasher,
                clock);
        updateService = new ArticleUpdateService(
                articleRepository,
                authorization,
                validator,
                passwordHasher,
                clock);
    }

    @Test
    void createsPublishedArticleWithCurrentPublishTime() {
        when(categoryRepository.findActiveByIdsForUpdate(List.of(10L)))
                .thenReturn(List.of(category(10L)));
        when(tagRepository.findActiveByIdsForUpdate(List.of(20L, 30L)))
                .thenReturn(List.of(tag(20L), tag(30L)));
        when(articleRepository.insert(any(NewArticle.class)))
                .thenAnswer(invocation -> articleFrom(
                        invocation.getArgument(0), 100L));

        ArticleResult result = createService.create(
                ADMIN,
                createCommand(
                        ArticleStatus.PUBLISHED,
                        null,
                        null,
                        List.of(30L, 20L)));

        assertThat(result.publishAt()).isEqualTo(NOW);
        assertThat(result.authorId()).isEqualTo(1001L);
        assertThat(result.tagIds()).containsExactly(20L, 30L);
        verify(articleRepository).replaceTags(100L, List.of(20L, 30L));
    }

    @Test
    void createsPublishedArticleWithDatabasePrecision() {
        Clock fractionalClock = Clock.fixed(
                Instant.parse("2026-06-15T03:00:00.800Z"),
                ZoneId.of("Asia/Tokyo"));
        ArticleCreateService fractionalClockService =
                new ArticleCreateService(
                        articleRepository,
                        new ContentAuthorization(),
                        new ArticleReferenceValidator(
                                categoryRepository,
                                tagRepository,
                                attachmentService),
                        passwordHasher,
                        fractionalClock);
        when(categoryRepository.findActiveByIdsForUpdate(List.of(10L)))
                .thenReturn(List.of(category(10L)));
        when(articleRepository.insert(any(NewArticle.class)))
                .thenAnswer(invocation -> articleFrom(
                        invocation.getArgument(0), 103L));

        fractionalClockService.create(
                ADMIN,
                createCommand(
                        ArticleStatus.PUBLISHED,
                        null,
                        null,
                        List.of()));

        ArgumentCaptor<NewArticle> articleCaptor =
                ArgumentCaptor.forClass(NewArticle.class);
        verify(articleRepository).insert(articleCaptor.capture());
        assertThat(articleCaptor.getValue().publishAt())
                .isEqualTo(NOW);
    }

    @Test
    void validatesCoverAndRejectsMissingReferencesBeforeInsert() {
        when(categoryRepository.findActiveByIdsForUpdate(List.of(10L)))
                .thenReturn(List.of(category(10L)));
        when(tagRepository.findActiveByIdsForUpdate(List.of(20L)))
                .thenReturn(List.of(tag(20L)));
        when(articleRepository.insert(any(NewArticle.class)))
                .thenAnswer(invocation -> articleFrom(
                        invocation.getArgument(0), 102L));

        createService.create(
                ADMIN,
                createCommand(
                        ArticleStatus.PUBLISHED,
                        100L,
                        null,
                        List.of(20L)));

        verify(attachmentService).requireActiveImageForUpdate(100L);
        org.mockito.Mockito.clearInvocations(articleRepository);

        when(tagRepository.findActiveByIdsForUpdate(List.of(404L)))
                .thenReturn(List.of());
        assertError(
                () -> createService.create(
                        ADMIN,
                        createCommand(
                                ArticleStatus.PUBLISHED,
                                null,
                                null,
                                List.of(404L))),
                ApiErrorCode.NOT_FOUND);
        verify(articleRepository, never()).insert(any());
    }

    @Test
    void hashesPasswordArticleAndRejectsDemoWrites() {
        when(categoryRepository.findActiveByIdsForUpdate(List.of(10L)))
                .thenReturn(List.of(category(10L)));
        when(passwordHasher.hash("secret"))
                .thenReturn("hash-secret");
        when(articleRepository.insert(any(NewArticle.class)))
                .thenAnswer(invocation -> articleFrom(
                        invocation.getArgument(0), 101L));

        ArticleResult result = createService.create(
                ADMIN,
                createCommand(
                        ArticleStatus.PASSWORD,
                        null,
                        "secret",
                        List.of()));

        assertThat(result.status()).isEqualTo(ArticleStatus.PASSWORD);
        ArgumentCaptor<NewArticle> articleCaptor =
                ArgumentCaptor.forClass(NewArticle.class);
        verify(articleRepository).insert(articleCaptor.capture());
        assertThat(articleCaptor.getValue().accessPassword())
                .isEqualTo("hash-secret");

        assertError(
                () -> createService.create(
                        DEMO,
                        createCommand(
                                ArticleStatus.PUBLISHED,
                                null,
                                null,
                                List.of())),
                ApiErrorCode.FORBIDDEN);
    }

    @Test
    void rejectsHomepageSlotForNonPublishedArticle() {
        assertError(
                () -> createService.create(
                        ADMIN,
                        createCommand(
                                ArticleStatus.DRAFT,
                                null,
                                null,
                                List.of(),
                                HomepageSlot.PINNED)),
                ApiErrorCode.VALIDATION_ERROR);

        verify(articleRepository, never()).insert(any());
    }

    @Test
    void mapsCreateDomainValidationFailureToApiValidationError() {
        when(categoryRepository.findActiveByIdsForUpdate(List.of(10L)))
                .thenReturn(List.of(category(10L)));

        assertError(
                () -> createService.create(
                        ADMIN,
                        new CreateArticleCommand(
                                " ", null, "Title", "摘要", null, null,
                                "正文", 10L, List.of(), "article",
                                ArticleStatus.PUBLISHED, null, null, null,
                                HomepageSlot.NONE)),
                ApiErrorCode.VALIDATION_ERROR);
    }

    @Test
    void mapsUpdateDomainValidationFailureToApiValidationError() {
        when(articleRepository.findActiveByIdForUpdate(100L))
                .thenReturn(Optional.of(currentArticle(
                        ArticleStatus.PUBLISHED,
                        null,
                        NOW.minusDays(1))));
        when(categoryRepository.findActiveByIdsForUpdate(List.of(10L)))
                .thenReturn(List.of(category(10L)));

        assertError(
                () -> updateService.update(
                        ADMIN,
                        100L,
                        new UpdateArticleCommand(
                                " ", null, "Title", "摘要", null, null,
                                "正文", 10L, List.of(), "article",
                                ArticleStatus.PUBLISHED, null, null, null,
                                HomepageSlot.NONE)),
                ApiErrorCode.VALIDATION_ERROR);
    }

    @Test
    void rejectsSecondPinnedHomepageArticle() {
        when(articleRepository.countActiveHomepageSlot(
                HomepageSlot.PINNED,
                null))
                .thenReturn(1);

        assertError(
                () -> createService.create(
                        ADMIN,
                        createCommand(
                                ArticleStatus.PUBLISHED,
                                null,
                                null,
                                List.of(),
                                HomepageSlot.PINNED)),
                ApiErrorCode.CONFLICT);

        verify(articleRepository, never()).insert(any());
    }

    @Test
    void clearsHomepageSlotWhenArticleBecomesNonPublished() {
        Article current = currentArticle(
                ArticleStatus.PUBLISHED,
                HomepageSlot.PINNED,
                null,
                NOW.minusDays(1));
        when(articleRepository.findActiveByIdForUpdate(100L))
                .thenReturn(Optional.of(current));
        when(categoryRepository.findActiveByIdsForUpdate(List.of(10L)))
                .thenReturn(List.of(category(10L)));
        when(articleRepository.update(any(), any(), any()))
                .thenReturn(true);

        updateService.update(
                ADMIN,
                100L,
                updateCommand(
                        ArticleStatus.PRIVATE,
                        null,
                        null,
                        List.of(),
                        HomepageSlot.PINNED));

        ArgumentCaptor<Article> articleCaptor =
                ArgumentCaptor.forClass(Article.class);
        verify(articleRepository).update(
                articleCaptor.capture(), any(), any());
        assertThat(articleCaptor.getValue().homepageSlot())
                .isEqualTo(HomepageSlot.NONE);
    }

    @Test
    void rejectsThirdFeaturedHomepageArticleOnUpdate() {
        Article current = currentArticle(
                ArticleStatus.PUBLISHED,
                HomepageSlot.NONE,
                null,
                NOW.minusDays(1));
        when(articleRepository.findActiveByIdForUpdate(100L))
                .thenReturn(Optional.of(current));
        when(articleRepository.countActiveHomepageSlot(
                HomepageSlot.FEATURED,
                100L))
                .thenReturn(2);

        assertError(
                () -> updateService.update(
                        ADMIN,
                        100L,
                        updateCommand(
                                ArticleStatus.PUBLISHED,
                                null,
                                null,
                                List.of(),
                                HomepageSlot.FEATURED)),
                ApiErrorCode.CONFLICT);

        verify(articleRepository, never()).update(any(), any(), any());
    }

    @Test
    void updatesPasswordHashByNullKeepingNewPasswordReplacingAndStatusLeavingClearing() {
        Article current = currentArticle(
                ArticleStatus.PASSWORD,
                "old-hash",
                NOW.minusDays(1));
        when(articleRepository.findActiveByIdForUpdate(100L))
                .thenReturn(Optional.of(current));
        when(categoryRepository.findActiveByIdsForUpdate(List.of(10L)))
                .thenReturn(List.of(category(10L)));
        when(tagRepository.findActiveByIdsForUpdate(List.of(20L, 30L)))
                .thenReturn(List.of(tag(20L), tag(30L)));
        when(articleRepository.update(any(), any(), any()))
                .thenReturn(true);

        updateService.update(
                ADMIN,
                100L,
                updateCommand(
                        ArticleStatus.PASSWORD,
                        null,
                        null,
                        List.of(30L, 20L)));

        ArgumentCaptor<Article> keepCaptor =
                ArgumentCaptor.forClass(Article.class);
        verify(articleRepository).update(
                keepCaptor.capture(), any(), any());
        assertThat(keepCaptor.getValue().accessPassword())
                .isEqualTo("old-hash");
        verify(articleRepository).replaceTags(100L, List.of(20L, 30L));
        org.mockito.Mockito.clearInvocations(articleRepository);

        when(articleRepository.findActiveByIdForUpdate(100L))
                .thenReturn(Optional.of(current));
        when(passwordHasher.hash("new-secret"))
                .thenReturn("new-hash");

        updateService.update(
                ADMIN,
                100L,
                updateCommand(
                        ArticleStatus.PASSWORD,
                        null,
                        "new-secret",
                        List.of()));

        ArgumentCaptor<Article> replaceCaptor =
                ArgumentCaptor.forClass(Article.class);
        verify(articleRepository)
                .update(replaceCaptor.capture(), any(), any());
        assertThat(replaceCaptor.getValue().accessPassword())
                .isEqualTo("new-hash");
        org.mockito.Mockito.clearInvocations(articleRepository);

        when(articleRepository.findActiveByIdForUpdate(100L))
                .thenReturn(Optional.of(current));
        updateService.update(
                ADMIN,
                100L,
                updateCommand(
                        ArticleStatus.PUBLISHED,
                        null,
                        null,
                        List.of()));
        ArgumentCaptor<Article> clearCaptor =
                ArgumentCaptor.forClass(Article.class);
        verify(articleRepository)
                .update(clearCaptor.capture(), any(), any());
        assertThat(clearCaptor.getValue().accessPassword())
                .isNull();
    }

    @Test
    void rejectsMissingArticleAndConflictUpdateRowCount() {
        when(articleRepository.findActiveByIdForUpdate(404L))
                .thenReturn(Optional.empty());

        assertError(
                () -> updateService.update(
                        ADMIN,
                        404L,
                        updateCommand(
                                ArticleStatus.PUBLISHED,
                                null,
                                null,
                                List.of())),
                ApiErrorCode.NOT_FOUND);

        when(articleRepository.findActiveByIdForUpdate(100L))
                .thenReturn(Optional.of(currentArticle(
                        ArticleStatus.PUBLISHED,
                        null,
                        NOW.minusDays(1))));
        when(categoryRepository.findActiveByIdsForUpdate(List.of(10L)))
                .thenReturn(List.of(category(10L)));
        when(articleRepository.update(any(), any(), any()))
                .thenReturn(false);

        assertError(
                () -> updateService.update(
                        ADMIN,
                        100L,
                        updateCommand(
                                ArticleStatus.PUBLISHED,
                                null,
                                null,
                                List.of())),
                ApiErrorCode.CONFLICT);
    }

    private static AuthenticatedPrincipal principal(
            String id,
            String role) {
        return new AuthenticatedPrincipal(
                id, role.toLowerCase(), List.of(role));
    }

    private static CreateArticleCommand createCommand(
            ArticleStatus status,
            Long coverAttachmentId,
            String password,
            List<Long> tagIds) {
        return createCommand(
                status,
                coverAttachmentId,
                password,
                tagIds,
                HomepageSlot.NONE);
    }

    private static CreateArticleCommand createCommand(
            ArticleStatus status,
            Long coverAttachmentId,
            String password,
            List<Long> tagIds,
            HomepageSlot homepageSlot) {
        return new CreateArticleCommand(
                "标题",
                null,
                "Title",
                "摘要",
                null,
                null,
                "正文",
                10L,
                tagIds,
                "article",
                status,
                password,
                null,
                coverAttachmentId,
                homepageSlot);
    }

    private static UpdateArticleCommand updateCommand(
            ArticleStatus status,
            LocalDateTime publishAt,
            String password,
            List<Long> tagIds) {
        return updateCommand(
                status,
                publishAt,
                password,
                tagIds,
                HomepageSlot.NONE);
    }

    private static UpdateArticleCommand updateCommand(
            ArticleStatus status,
            LocalDateTime publishAt,
            String password,
            List<Long> tagIds,
            HomepageSlot homepageSlot) {
        return new UpdateArticleCommand(
                "新标题",
                null,
                "New title",
                "新摘要",
                null,
                null,
                "新正文",
                10L,
                tagIds,
                "article-updated",
                status,
                password,
                publishAt,
                null,
                homepageSlot);
    }

    private Article articleFrom(NewArticle article, long id) {
        return Article.reconstitute(
                id,
                article.titleZh(),
                article.titleJa(),
                article.titleEn(),
                article.summaryZh(),
                article.summaryJa(),
                article.summaryEn(),
                article.body(),
                article.categoryId(),
                article.authorId(),
                article.slug(),
                article.status(),
                article.accessPassword(),
                article.publishAt(),
                article.coverAttachmentId(),
                article.commentCount(),
                article.tagIds(),
                article.createdAt(),
                article.createdBy(),
                article.createdAt(),
                article.createdBy(),
                false,
                null,
                null);
    }

    private static Article currentArticle(
            ArticleStatus status,
            String passwordHash,
            LocalDateTime publishAt) {
        return currentArticle(
                status,
                HomepageSlot.NONE,
                passwordHash,
                publishAt);
    }

    private static Article currentArticle(
            ArticleStatus status,
            HomepageSlot homepageSlot,
            String passwordHash,
            LocalDateTime publishAt) {
        return Article.reconstitute(
                100L,
                "旧标题",
                null,
                null,
                "旧摘要",
                null,
                null,
                "旧正文",
                10L,
                1001L,
                "article",
                status,
                homepageSlot,
                passwordHash,
                publishAt,
                null,
                2,
                List.of(20L),
                NOW.minusDays(10),
                1001L,
                NOW.minusDays(2),
                1001L,
                false,
                null,
                null);
    }

    private static Category category(long id) {
        return Category.reconstitute(
                id,
                "分类",
                null,
                null,
                "category-" + id,
                10,
                NOW.minusDays(1),
                1001L,
                NOW.minusHours(1),
                1001L);
    }

    private static Tag tag(long id) {
        return Tag.reconstitute(
                id,
                "标签" + id,
                null,
                null,
                "tag-" + id,
                NOW.minusDays(1),
                1001L,
                NOW.minusHours(1),
                1001L);
    }

    private static void assertError(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable callable,
            ApiErrorCode code) {
        assertThatThrownBy(callable)
                .isInstanceOfSatisfying(
                        ApiException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(code));
    }
}
