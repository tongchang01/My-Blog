package com.tyb.myblog.v2.content.application;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.category.CategoryCreateService;
import com.tyb.myblog.v2.content.application.category.CategoryUpdateService;
import com.tyb.myblog.v2.content.application.category.CreateCategoryCommand;
import com.tyb.myblog.v2.content.application.category.UpdateCategoryCommand;
import com.tyb.myblog.v2.content.application.tag.CreateTagCommand;
import com.tyb.myblog.v2.content.application.tag.TagCreateService;
import com.tyb.myblog.v2.content.application.tag.TagUpdateService;
import com.tyb.myblog.v2.content.application.tag.UpdateTagCommand;
import com.tyb.myblog.v2.content.domain.ContentSlugConflictException;
import com.tyb.myblog.v2.content.domain.category.Category;
import com.tyb.myblog.v2.content.domain.category.CategoryRepository;
import com.tyb.myblog.v2.content.domain.category.NewCategory;
import com.tyb.myblog.v2.content.domain.tag.NewTag;
import com.tyb.myblog.v2.content.domain.tag.Tag;
import com.tyb.myblog.v2.content.domain.tag.TagRepository;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryTagWriteServiceTest {

    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 6, 15, 12, 0);

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TagRepository tagRepository;

    private CategoryCreateService categoryCreateService;
    private CategoryUpdateService categoryUpdateService;
    private TagCreateService tagCreateService;
    private TagUpdateService tagUpdateService;

    @BeforeEach
    void setUp() {
        ContentAuthorization authorization =
                new ContentAuthorization();
        Clock clock = Clock.fixed(
                Instant.parse("2026-06-15T03:00:00Z"),
                ZoneId.of("Asia/Tokyo"));
        categoryCreateService = new CategoryCreateService(
                categoryRepository, authorization);
        categoryUpdateService = new CategoryUpdateService(
                categoryRepository, authorization, clock);
        tagCreateService = new TagCreateService(
                tagRepository, authorization);
        tagUpdateService = new TagUpdateService(
                tagRepository, authorization, clock);
    }

    @Test
    void allowsAdminToCreateCategoryAndTag() {
        when(categoryRepository.findBySlugIncludingDeleted("backend"))
                .thenReturn(Optional.empty());
        when(categoryRepository.insert(any(NewCategory.class)))
                .thenReturn(category(101L, "backend", "后端", null, 10));
        when(tagRepository.findBySlugIncludingDeleted("java"))
                .thenReturn(Optional.empty());
        when(tagRepository.insert(any(NewTag.class)))
                .thenReturn(tag(201L, "java", "Java", null));

        assertThat(categoryCreateService.create(
                principal("1001", "ADMIN"),
                new CreateCategoryCommand(
                        "后端", null, "Backend", " Backend ", 10)))
                .satisfies(result -> {
                    assertThat(result.slug()).isEqualTo("backend");
                    assertThat(result.createdBy()).isEqualTo(1001L);
                });
        assertThat(tagCreateService.create(
                principal("1001", "ADMIN"),
                new CreateTagCommand(
                        "Java", null, null, "JAVA")))
                .satisfies(result ->
                        assertThat(result.slug()).isEqualTo("java"));
    }

    @Test
    void rejectsDemoAndInvalidAdminActor() {
        assertError(
                () -> categoryCreateService.create(
                        principal("1001", "DEMO"),
                        new CreateCategoryCommand(
                                "后端", null, null, "backend", 10)),
                ApiErrorCode.FORBIDDEN);
        assertError(
                () -> tagCreateService.create(
                        principal("0", "ADMIN"),
                        new CreateTagCommand(
                                "Java", null, null, "java")),
                ApiErrorCode.FORBIDDEN);
    }

    @Test
    void rejectsSlugOccupiedByActiveOrDeletedRows() {
        when(categoryRepository.findBySlugIncludingDeleted("existing"))
                .thenReturn(Optional.of(
                        category(101L, "existing", "旧分类", null, 10)));
        when(tagRepository.findBySlugIncludingDeleted("deleted-tag"))
                .thenReturn(Optional.of(
                        tag(201L, "deleted-tag", "旧标签", null)));

        assertError(
                () -> categoryCreateService.create(
                        principal("1001", "ADMIN"),
                        new CreateCategoryCommand(
                                "后端", null, null, "existing", 10)),
                ApiErrorCode.CONFLICT);
        assertError(
                () -> tagCreateService.create(
                        principal("1001", "ADMIN"),
                        new CreateTagCommand(
                                "Java", null, null, "deleted-tag")),
                ApiErrorCode.CONFLICT);
    }

    @Test
    void mapsConcurrentUniqueConflictsToConflict() {
        when(categoryRepository.findBySlugIncludingDeleted("backend"))
                .thenReturn(Optional.empty());
        when(categoryRepository.insert(any(NewCategory.class)))
                .thenThrow(new ContentSlugConflictException());

        assertError(
                () -> categoryCreateService.create(
                        principal("1001", "ADMIN"),
                        new CreateCategoryCommand(
                                "后端", null, null, "backend", 10)),
                ApiErrorCode.CONFLICT);
    }

    @Test
    void fullyUpdatesCategoryAndAllowsExplicitLanguageClearing() {
        Category current =
                category(101L, "backend", "后端", "バックエンド", 10);
        when(categoryRepository.findActiveByIdForUpdate(101L))
                .thenReturn(Optional.of(current));
        when(categoryRepository.findBySlugIncludingDeleted("server"))
                .thenReturn(Optional.empty());
        when(categoryRepository.update(
                any(Category.class), any(LocalDateTime.class), anyLong()))
                .thenReturn(true);
        when(categoryRepository.findActiveById(101L))
                .thenReturn(Optional.of(
                        category(101L, "server", "服务端", null, 20)));

        categoryUpdateService.update(
                principal("1001", "ADMIN"),
                101L,
                new UpdateCategoryCommand(
                        "服务端", null, null, "server", 20));

        ArgumentCaptor<Category> categoryCaptor =
                ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).update(
                categoryCaptor.capture(), any(LocalDateTime.class), anyLong());
        assertThat(categoryCaptor.getValue().name().ja()).isNull();
        assertThat(categoryCaptor.getValue().name().en()).isNull();
        assertThat(categoryCaptor.getValue().sortOrder()).isEqualTo(20);
    }

    @Test
    void permitsKeepingOwnSlugDuringTagUpdate() {
        Tag current = tag(201L, "java", "Java", "ジャバ");
        when(tagRepository.findActiveByIdForUpdate(201L))
                .thenReturn(Optional.of(current));
        when(tagRepository.findBySlugIncludingDeleted("java"))
                .thenReturn(Optional.of(current));
        when(tagRepository.update(
                any(Tag.class), any(LocalDateTime.class), anyLong()))
                .thenReturn(true);
        when(tagRepository.findActiveById(201L))
                .thenReturn(Optional.of(
                        tag(201L, "java", "Java 17", null)));

        tagUpdateService.update(
                principal("1001", "ADMIN"),
                201L,
                new UpdateTagCommand(
                        "Java 17", null, null, "java"));

        verify(tagRepository).findActiveByIdForUpdate(201L);
        verify(tagRepository).update(
                any(Tag.class), any(LocalDateTime.class), anyLong());
    }

    @Test
    void rejectsMissingUpdateTargetsBeforeWriting() {
        when(categoryRepository.findActiveByIdForUpdate(999L))
                .thenReturn(Optional.empty());

        assertError(
                () -> categoryUpdateService.update(
                        principal("1001", "ADMIN"),
                        999L,
                        new UpdateCategoryCommand(
                                "后端", null, null, "backend", 10)),
                ApiErrorCode.NOT_FOUND);
        verify(categoryRepository, never())
                .update(any(), any(), anyLong());
    }

    @Test
    void mapsUnexpectedUpdateRowCountToInternalError() {
        Tag current = tag(201L, "java", "Java", null);
        when(tagRepository.findActiveByIdForUpdate(201L))
                .thenReturn(Optional.of(current));
        when(tagRepository.findBySlugIncludingDeleted("java"))
                .thenReturn(Optional.of(current));
        when(tagRepository.update(
                any(Tag.class), any(LocalDateTime.class), anyLong()))
                .thenReturn(false);

        assertError(
                () -> tagUpdateService.update(
                        principal("1001", "ADMIN"),
                        201L,
                        new UpdateTagCommand(
                                "Java", null, null, "java")),
                ApiErrorCode.INTERNAL_ERROR);
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

    private AuthenticatedPrincipal principal(
            String id,
            String role) {
        return new AuthenticatedPrincipal(
                id, role.toLowerCase(), List.of(role));
    }

    private Category category(
            long id,
            String slug,
            String nameZh,
            String nameJa,
            int sortOrder) {
        return Category.reconstitute(
                id,
                nameZh,
                nameJa,
                null,
                slug,
                sortOrder,
                NOW.minusHours(2),
                1001L,
                NOW.minusHours(1),
                1001L);
    }

    private Tag tag(
            long id,
            String slug,
            String nameZh,
            String nameJa) {
        return Tag.reconstitute(
                id,
                nameZh,
                nameJa,
                null,
                slug,
                NOW.minusHours(2),
                1001L,
                NOW.minusHours(1),
                1001L);
    }
}
