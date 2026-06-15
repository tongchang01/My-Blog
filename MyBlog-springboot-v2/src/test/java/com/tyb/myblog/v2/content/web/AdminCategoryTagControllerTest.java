package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.GlobalExceptionHandler;
import com.tyb.myblog.v2.content.application.category.CategoryCreateService;
import com.tyb.myblog.v2.content.application.category.CategoryDeleteService;
import com.tyb.myblog.v2.content.application.category.CategoryQueryService;
import com.tyb.myblog.v2.content.application.category.CategoryResult;
import com.tyb.myblog.v2.content.application.category.CategorySortItem;
import com.tyb.myblog.v2.content.application.category.CategorySortService;
import com.tyb.myblog.v2.content.application.category.CategoryUpdateService;
import com.tyb.myblog.v2.content.application.category.CreateCategoryCommand;
import com.tyb.myblog.v2.content.application.category.UpdateCategoryCommand;
import com.tyb.myblog.v2.content.application.category.UpdateCategorySortOrdersCommand;
import com.tyb.myblog.v2.content.application.tag.CreateTagCommand;
import com.tyb.myblog.v2.content.application.tag.TagCreateService;
import com.tyb.myblog.v2.content.application.tag.TagDeleteService;
import com.tyb.myblog.v2.content.application.tag.TagQueryService;
import com.tyb.myblog.v2.content.application.tag.TagResult;
import com.tyb.myblog.v2.content.application.tag.TagUpdateService;
import com.tyb.myblog.v2.content.application.tag.UpdateTagCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
        AdminCategoryController.class,
        AdminTagController.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminCategoryTagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryQueryService categoryService;

    @MockitoBean
    private CategoryCreateService categoryCreateService;

    @MockitoBean
    private CategoryUpdateService categoryUpdateService;

    @MockitoBean
    private CategorySortService categorySortService;

    @MockitoBean
    private CategoryDeleteService categoryDeleteService;

    @MockitoBean
    private TagQueryService tagService;

    @MockitoBean
    private TagCreateService tagCreateService;

    @MockitoBean
    private TagUpdateService tagUpdateService;

    @MockitoBean
    private TagDeleteService tagDeleteService;

    @MockitoBean
    private CategoryWebMapping categoryMapping;

    @MockitoBean
    private TagWebMapping tagMapping;

    private AuthenticatedPrincipal principal;

    @BeforeEach
    void setUp() {
        principal = new AuthenticatedPrincipal(
                "1001", "admin", List.of("ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal, null, List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsCompleteCategoryListAndDetail() throws Exception {
        CategoryResult result = categoryResult();
        AdminCategoryVO vo = new AdminCategoryVO(
                result.id(),
                result.nameZh(),
                result.nameJa(),
                result.nameEn(),
                result.slug(),
                result.sortOrder(),
                result.createdAt(),
                result.createdBy(),
                result.updatedAt(),
                result.updatedBy());
        when(categoryService.adminList(principal))
                .thenReturn(List.of(result));
        when(categoryService.adminDetail(principal, 101L))
                .thenReturn(result);
        when(categoryMapping.toAdminVO(result)).thenReturn(vo);

        mockMvc.perform(get("/api/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].nameZh")
                        .value("后端"))
                .andExpect(jsonPath("$.data[0].sortOrder")
                        .value(10))
                .andExpect(jsonPath("$.data[0].createdBy")
                        .value(1001))
                .andExpect(jsonPath("$.data[0].deleted")
                        .doesNotExist());
        mockMvc.perform(get("/api/admin/categories/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(101));
    }

    @Test
    void returnsCompleteTagListAndDetail() throws Exception {
        TagResult result = tagResult();
        AdminTagVO vo = new AdminTagVO(
                result.id(),
                result.nameZh(),
                result.nameJa(),
                result.nameEn(),
                result.slug(),
                result.createdAt(),
                result.createdBy(),
                result.updatedAt(),
                result.updatedBy());
        when(tagService.adminList(principal))
                .thenReturn(List.of(result));
        when(tagService.adminDetail(principal, 201L))
                .thenReturn(result);
        when(tagMapping.toAdminVO(result)).thenReturn(vo);

        mockMvc.perform(get("/api/admin/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].nameZh")
                        .value("Java"))
                .andExpect(jsonPath("$.data[0].createdAt")
                        .value("2026-06-15T10:00:00"))
                .andExpect(jsonPath("$.data[0].deletedAt")
                        .doesNotExist());
        mockMvc.perform(get("/api/admin/tags/201"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(201));
    }

    @Test
    void createsAndFullyUpdatesCategory() throws Exception {
        CategoryResult result = categoryResult();
        when(categoryCreateService.create(
                any(), any(CreateCategoryCommand.class)))
                .thenReturn(result);
        when(categoryUpdateService.update(
                any(), any(Long.class), any(UpdateCategoryCommand.class)))
                .thenReturn(result);
        when(categoryMapping.toAdminVO(result))
                .thenReturn(categoryVO(result));

        String body = """
                {
                  "nameZh":"后端",
                  "nameJa":null,
                  "nameEn":"Backend",
                  "slug":"backend",
                  "sortOrder":10
                }
                """;
        mockMvc.perform(post("/api/admin/categories")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("backend"));
        mockMvc.perform(put("/api/admin/categories/101")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk());

        verify(categoryCreateService).create(
                principal,
                new CreateCategoryCommand(
                        "后端", null, "Backend", "backend", 10));
        verify(categoryUpdateService).update(
                principal,
                101L,
                new UpdateCategoryCommand(
                        "后端", null, "Backend", "backend", 10));
    }

    @Test
    void createsAndFullyUpdatesTagWithExplicitNullNames()
            throws Exception {
        TagResult result = tagResult();
        when(tagCreateService.create(
                any(), any(CreateTagCommand.class)))
                .thenReturn(result);
        when(tagUpdateService.update(
                any(), any(Long.class), any(UpdateTagCommand.class)))
                .thenReturn(result);
        when(tagMapping.toAdminVO(result)).thenReturn(tagVO(result));

        String body = """
                {
                  "nameZh":"Java",
                  "nameJa":null,
                  "nameEn":null,
                  "slug":"java"
                }
                """;
        mockMvc.perform(post("/api/admin/tags")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/admin/tags/201")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk());

        verify(tagUpdateService).update(
                principal,
                201L,
                new UpdateTagCommand("Java", null, null, "java"));
    }

    @Test
    void rejectsMissingUnknownAndInvalidCategoryFields()
            throws Exception {
        mockMvc.perform(post("/api/admin/categories")
                        .contentType("application/json")
                        .content("""
                                {
                                  "nameZh":"后端",
                                  "nameJa":null,
                                  "nameEn":null,
                                  "slug":"backend"
                                }
                                """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/admin/categories")
                        .contentType("application/json")
                        .content("""
                                {
                                  "nameZh":"后端",
                                  "nameJa":null,
                                  "nameEn":null,
                                  "slug":"backend",
                                  "sortOrder":10,
                                  "unknown":true
                                }
                                """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/admin/categories")
                        .contentType("application/json")
                        .content("""
                                {
                                  "nameZh":"后端",
                                  "nameJa":null,
                                  "nameEn":null,
                                  "slug":"invalid_slug",
                                  "sortOrder":-1
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMissingAndOverlongTagFields() throws Exception {
        mockMvc.perform(put("/api/admin/tags/201")
                        .contentType("application/json")
                        .content("""
                                {
                                  "nameZh":"Java",
                                  "nameJa":null,
                                  "slug":"java"
                                }
                                """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/admin/tags")
                        .contentType("application/json")
                        .content("""
                                {
                                  "nameZh":"%s",
                                  "nameJa":null,
                                  "nameEn":null,
                                  "slug":"java"
                                }
                                """.formatted("a".repeat(65))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sortsCategoriesOnStaticPathAndDeletesByNumericIdentity()
            throws Exception {
        mockMvc.perform(put("/api/admin/categories/sort-orders")
                        .contentType("application/json")
                        .content("""
                                {
                                  "items":[
                                    {"id":101,"sortOrder":0},
                                    {"id":102,"sortOrder":10}
                                  ]
                                }
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/admin/categories/101"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/admin/tags/201"))
                .andExpect(status().isOk());

        verify(categorySortService).update(
                principal,
                new UpdateCategorySortOrdersCommand(List.of(
                        new CategorySortItem(101L, 0),
                        new CategorySortItem(102L, 10))));
        verify(categoryDeleteService).delete(principal, 101L);
        verify(tagDeleteService).delete(principal, 201L);
    }

    private AdminCategoryVO categoryVO(CategoryResult result) {
        return new AdminCategoryVO(
                result.id(),
                result.nameZh(),
                result.nameJa(),
                result.nameEn(),
                result.slug(),
                result.sortOrder(),
                result.createdAt(),
                result.createdBy(),
                result.updatedAt(),
                result.updatedBy());
    }

    private AdminTagVO tagVO(TagResult result) {
        return new AdminTagVO(
                result.id(),
                result.nameZh(),
                result.nameJa(),
                result.nameEn(),
                result.slug(),
                result.createdAt(),
                result.createdBy(),
                result.updatedAt(),
                result.updatedBy());
    }

    private CategoryResult categoryResult() {
        return new CategoryResult(
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

    private TagResult tagResult() {
        return new TagResult(
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
