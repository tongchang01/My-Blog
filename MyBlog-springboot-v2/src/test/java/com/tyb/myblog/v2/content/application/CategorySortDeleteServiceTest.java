package com.tyb.myblog.v2.content.application;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.category.CategoryDeleteService;
import com.tyb.myblog.v2.content.application.category.CategorySortItem;
import com.tyb.myblog.v2.content.application.category.CategorySortService;
import com.tyb.myblog.v2.content.application.category.UpdateCategorySortOrdersCommand;
import com.tyb.myblog.v2.content.application.tag.TagDeleteService;
import com.tyb.myblog.v2.content.domain.category.Category;
import com.tyb.myblog.v2.content.domain.category.CategoryRepository;
import com.tyb.myblog.v2.content.domain.tag.Tag;
import com.tyb.myblog.v2.content.domain.tag.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategorySortDeleteServiceTest {

    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 6, 15, 12, 0);

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TagRepository tagRepository;

    private CategorySortService sortService;
    private CategoryDeleteService categoryDeleteService;
    private TagDeleteService tagDeleteService;

    @BeforeEach
    void setUp() {
        ContentAuthorization authorization =
                new ContentAuthorization();
        Clock clock = Clock.fixed(
                Instant.parse("2026-06-15T03:00:00Z"),
                ZoneId.of("Asia/Tokyo"));
        sortService = new CategorySortService(
                categoryRepository, authorization, clock);
        categoryDeleteService = new CategoryDeleteService(
                categoryRepository, authorization, clock);
        tagDeleteService = new TagDeleteService(
                tagRepository, authorization, clock);
    }

    @Test
    void sortsAfterLockingIdsInAscendingOrder() {
        List<CategorySortItem> items = List.of(
                new CategorySortItem(20L, 10),
                new CategorySortItem(10L, 10));
        when(categoryRepository.findActiveByIdsForUpdate(
                List.of(10L, 20L)))
                .thenReturn(List.of(
                        category(10L, 0),
                        category(20L, 0)));
        when(categoryRepository.updateSortOrder(
                anyLong(), anyInt(), any(), anyLong()))
                .thenReturn(true);

        sortService.update(
                principal("1001", "ADMIN"),
                new UpdateCategorySortOrdersCommand(items));

        InOrder order = inOrder(categoryRepository);
        order.verify(categoryRepository)
                .findActiveByIdsForUpdate(List.of(10L, 20L));
        order.verify(categoryRepository)
                .updateSortOrder(20L, 10, NOW, 1001L);
        order.verify(categoryRepository)
                .updateSortOrder(10L, 10, NOW, 1001L);
    }

    @Test
    void rejectsInvalidSortRequestsBeforeLocking() {
        assertSortError(null);
        assertSortError(List.of());
        assertSortError(IntStream.rangeClosed(1, 101)
                .mapToObj(id -> new CategorySortItem(id, id))
                .toList());
        assertSortError(List.of(new CategorySortItem(0, 1)));
        assertSortError(List.of(new CategorySortItem(1, -1)));
        assertSortError(List.of(
                new CategorySortItem(1, 1),
                new CategorySortItem(1, 2)));
        assertSortError(List.of(
                new CategorySortItem(1, 1_000_001)));

        verify(categoryRepository, never())
                .findActiveByIdsForUpdate(any());
    }

    @Test
    void failsWholeSortWhenTargetMissingOrUpdateFails() {
        List<CategorySortItem> items = List.of(
                new CategorySortItem(10L, 1),
                new CategorySortItem(20L, 2));
        when(categoryRepository.findActiveByIdsForUpdate(
                List.of(10L, 20L)))
                .thenReturn(List.of(category(10L, 0)));

        assertError(
                () -> sortService.update(
                        principal("1001", "ADMIN"),
                        new UpdateCategorySortOrdersCommand(items)),
                ApiErrorCode.NOT_FOUND);
        verify(categoryRepository, never())
                .updateSortOrder(
                        anyLong(), anyInt(), any(), anyLong());

        when(categoryRepository.findActiveByIdsForUpdate(
                List.of(10L, 20L)))
                .thenReturn(List.of(
                        category(10L, 0),
                        category(20L, 0)));
        when(categoryRepository.updateSortOrder(
                10L, 1, NOW, 1001L)).thenReturn(false);
        assertError(
                () -> sortService.update(
                        principal("1001", "ADMIN"),
                        new UpdateCategorySortOrdersCommand(items)),
                ApiErrorCode.INTERNAL_ERROR);
    }

    @Test
    void rejectsReferencedCategoryAndTagWithoutDeleting() {
        when(categoryRepository.findActiveByIdForUpdate(10L))
                .thenReturn(Optional.of(category(10L, 0)));
        when(categoryRepository.hasActiveArticleReference(10L))
                .thenReturn(true);
        when(tagRepository.findActiveByIdForUpdate(20L))
                .thenReturn(Optional.of(tag(20L)));
        when(tagRepository.hasActiveArticleReference(20L))
                .thenReturn(true);

        assertError(
                () -> categoryDeleteService.delete(
                        principal("1001", "ADMIN"), 10L),
                ApiErrorCode.CONFLICT);
        assertError(
                () -> tagDeleteService.delete(
                        principal("1001", "ADMIN"), 20L),
                ApiErrorCode.CONFLICT);
        verify(categoryRepository, never())
                .softDelete(anyLong(), any(), anyLong());
        verify(tagRepository, never())
                .softDelete(anyLong(), any(), anyLong());
    }

    @Test
    void softDeletesUnreferencedCategoryAndTag() {
        when(categoryRepository.findActiveByIdForUpdate(10L))
                .thenReturn(Optional.of(category(10L, 0)));
        when(categoryRepository.hasActiveArticleReference(10L))
                .thenReturn(false);
        when(categoryRepository.softDelete(10L, NOW, 1001L))
                .thenReturn(true);
        when(tagRepository.findActiveByIdForUpdate(20L))
                .thenReturn(Optional.of(tag(20L)));
        when(tagRepository.hasActiveArticleReference(20L))
                .thenReturn(false);
        when(tagRepository.softDelete(20L, NOW, 1001L))
                .thenReturn(true);

        categoryDeleteService.delete(
                principal("1001", "ADMIN"), 10L);
        tagDeleteService.delete(
                principal("1001", "ADMIN"), 20L);

        verify(categoryRepository).softDelete(
                10L, NOW, 1001L);
        verify(tagRepository).softDelete(
                20L, NOW, 1001L);
    }

    @Test
    void validatesDeleteAuthorizationIdentityAndTarget() {
        assertError(
                () -> categoryDeleteService.delete(
                        principal("1001", "DEMO"), 10L),
                ApiErrorCode.FORBIDDEN);
        assertError(
                () -> tagDeleteService.delete(
                        principal("0", "ADMIN"), 20L),
                ApiErrorCode.FORBIDDEN);
        assertError(
                () -> categoryDeleteService.delete(
                        principal("1001", "ADMIN"), 0L),
                ApiErrorCode.VALIDATION_ERROR);
        when(categoryRepository.findActiveByIdForUpdate(10L))
                .thenReturn(Optional.empty());
        assertError(
                () -> categoryDeleteService.delete(
                        principal("1001", "ADMIN"), 10L),
                ApiErrorCode.NOT_FOUND);
    }

    private void assertSortError(List<CategorySortItem> items) {
        assertError(
                () -> sortService.update(
                        principal("1001", "ADMIN"),
                        new UpdateCategorySortOrdersCommand(items)),
                ApiErrorCode.VALIDATION_ERROR);
    }

    private void assertError(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable callable,
            ApiErrorCode code) {
        assertThatThrownBy(callable)
                .isInstanceOfSatisfying(
                        ApiException.class,
                        exception -> org.assertj.core.api.Assertions
                                .assertThat(exception.code())
                                .isEqualTo(code));
    }

    private AuthenticatedPrincipal principal(
            String id,
            String role) {
        return new AuthenticatedPrincipal(
                id, role.toLowerCase(), List.of(role));
    }

    private Category category(long id, int sortOrder) {
        return Category.reconstitute(
                id,
                "分类" + id,
                null,
                null,
                "category-" + id,
                sortOrder,
                NOW.minusHours(2),
                1001L,
                NOW.minusHours(1),
                1001L);
    }

    private Tag tag(long id) {
        return Tag.reconstitute(
                id,
                "标签" + id,
                null,
                null,
                "tag-" + id,
                NOW.minusHours(2),
                1001L,
                NOW.minusHours(1),
                1001L);
    }
}
