package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.GlobalExceptionHandler;
import com.tyb.myblog.v2.content.application.category.CategoryQueryService;
import com.tyb.myblog.v2.content.application.category.CategoryResult;
import com.tyb.myblog.v2.content.application.tag.TagQueryService;
import com.tyb.myblog.v2.content.application.tag.TagResult;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    private TagQueryService tagService;

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
