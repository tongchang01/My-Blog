package com.tyb.myblog.v2.content.application;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.category.CategoryQueryService;
import com.tyb.myblog.v2.content.application.category.PublicCategoryResult;
import com.tyb.myblog.v2.content.application.tag.PublicTagResult;
import com.tyb.myblog.v2.content.application.tag.TagQueryService;
import com.tyb.myblog.v2.content.domain.category.Category;
import com.tyb.myblog.v2.content.domain.category.CategoryRepository;
import com.tyb.myblog.v2.content.domain.tag.Tag;
import com.tyb.myblog.v2.content.domain.tag.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryTagQueryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TagRepository tagRepository;

    private CategoryQueryService categoryService;
    private TagQueryService tagService;

    @BeforeEach
    void setUp() {
        ContentAuthorization authorization =
                new ContentAuthorization();
        categoryService = new CategoryQueryService(
                categoryRepository, authorization);
        tagService = new TagQueryService(
                tagRepository, authorization);
    }

    @Test
    void returnsLocalizedPublicResultsWithChineseFallback() {
        when(categoryRepository.findAllActive())
                .thenReturn(List.of(category()));
        when(tagRepository.findAllActive())
                .thenReturn(List.of(tag()));

        assertThat(categoryService.publicList("ja"))
                .containsExactly(new PublicCategoryResult(
                        101L, "后端", "backend"));
        assertThat(categoryService.publicList("en"))
                .containsExactly(new PublicCategoryResult(
                        101L, "Backend", "backend"));
        assertThat(tagService.publicList("ja"))
                .containsExactly(new PublicTagResult(
                        201L, "Java", "java"));
    }

    @Test
    void rejectsMissingAndUnsupportedPublicLanguages() {
        for (String value : new String[]{null, "", "ZH", "fr"}) {
            assertError(
                    () -> categoryService.publicList(value),
                    ApiErrorCode.VALIDATION_ERROR);
            assertError(
                    () -> tagService.publicList(value),
                    ApiErrorCode.VALIDATION_ERROR);
        }
    }

    @Test
    void allowsAdminAndDemoToReadCompleteAdminResults() {
        when(categoryRepository.findAllActive())
                .thenReturn(List.of(category()));
        when(tagRepository.findAllActive())
                .thenReturn(List.of(tag()));

        assertThat(categoryService.adminList(principal("ADMIN")))
                .singleElement()
                .satisfies(result -> {
                    assertThat(result.nameZh()).isEqualTo("后端");
                    assertThat(result.nameEn()).isEqualTo("Backend");
                    assertThat(result.sortOrder()).isEqualTo(10);
                });
        assertThat(tagService.adminList(principal("DEMO")))
                .singleElement()
                .satisfies(result -> {
                    assertThat(result.nameZh()).isEqualTo("Java");
                    assertThat(result.nameJa()).isNull();
                });
    }

    @Test
    void rejectsMissingAndUnreadableAdminPrincipal() {
        assertError(
                () -> categoryService.adminList(null),
                ApiErrorCode.INVALID_TOKEN);
        assertError(
                () -> tagService.adminList(principal("GUEST")),
                ApiErrorCode.FORBIDDEN);
    }

    @Test
    void returnsAdminDetailsAndMapsMissingRowsToNotFound() {
        when(categoryRepository.findActiveById(101L))
                .thenReturn(Optional.of(category()));
        when(categoryRepository.findActiveById(999L))
                .thenReturn(Optional.empty());
        when(tagRepository.findActiveById(201L))
                .thenReturn(Optional.of(tag()));

        assertThat(categoryService.adminDetail(
                principal("DEMO"), 101L).id()).isEqualTo(101L);
        assertThat(tagService.adminDetail(
                principal("ADMIN"), 201L).id()).isEqualTo(201L);
        assertError(
                () -> categoryService.adminDetail(
                        principal("ADMIN"), 999L),
                ApiErrorCode.NOT_FOUND);
    }

    @Test
    void rejectsNonPositiveAdminDetailIdentity() {
        assertError(
                () -> categoryService.adminDetail(
                        principal("ADMIN"), 0L),
                ApiErrorCode.VALIDATION_ERROR);
        assertError(
                () -> tagService.adminDetail(
                        principal("ADMIN"), -1L),
                ApiErrorCode.VALIDATION_ERROR);
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

    private AuthenticatedPrincipal principal(String role) {
        return new AuthenticatedPrincipal(
                "1001", role.toLowerCase(), List.of(role));
    }

    private Category category() {
        return Category.reconstitute(
                101L,
                "后端",
                null,
                "Backend",
                "backend",
                10,
                LocalDateTime.of(2026, 6, 15, 10, 0),
                1001L,
                LocalDateTime.of(2026, 6, 15, 11, 0),
                1001L);
    }

    private Tag tag() {
        return Tag.reconstitute(
                201L,
                "Java",
                null,
                null,
                "java",
                LocalDateTime.of(2026, 6, 15, 10, 0),
                1001L,
                LocalDateTime.of(2026, 6, 15, 11, 0),
                1001L);
    }
}
