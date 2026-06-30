package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.common.error.GlobalExceptionHandler;
import com.tyb.myblog.v2.content.application.category.CategoryQueryService;
import com.tyb.myblog.v2.content.application.category.PublicCategoryResult;
import com.tyb.myblog.v2.content.application.tag.PublicTagResult;
import com.tyb.myblog.v2.content.application.tag.TagQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
        PublicCategoryController.class,
        PublicTagController.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PublicCategoryTagControllerTest {

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

    @Test
    void returnsOnlyLocalizedPublicCategoryFields() throws Exception {
        PublicCategoryResult result =
                new PublicCategoryResult(
                        101L, "后端", "backend");
        when(categoryService.publicList("zh"))
                .thenReturn(List.of(result));
        when(categoryMapping.toPublicVO(result))
                .thenReturn(new PublicCategoryVO(
                        "101", "后端", "backend"));

        mockMvc.perform(get("/api/public/categories")
                        .queryParam("lang", "zh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("101"))
                .andExpect(jsonPath("$.data[0].name").value("后端"))
                .andExpect(jsonPath("$.data[0].slug")
                        .value("backend"))
                .andExpect(jsonPath("$.data[0].nameZh")
                        .doesNotExist())
                .andExpect(jsonPath("$.data[0].sortOrder")
                        .doesNotExist())
                .andExpect(jsonPath("$.data[0].createdAt")
                        .doesNotExist())
                .andExpect(jsonPath("$.data[0].deleted")
                        .doesNotExist());
    }

    @Test
    void returnsOnlyLocalizedPublicTagFields() throws Exception {
        PublicTagResult result =
                new PublicTagResult(201L, "Java", "java");
        when(tagService.publicList("en"))
                .thenReturn(List.of(result));
        when(tagMapping.toPublicVO(result))
                .thenReturn(new PublicTagVO(
                        "201", "Java", "java"));

        mockMvc.perform(get("/api/public/tags")
                        .queryParam("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("201"))
                .andExpect(jsonPath("$.data[0].name").value("Java"))
                .andExpect(jsonPath("$.data[0].slug").value("java"))
                .andExpect(jsonPath("$.data[0].nameEn")
                        .doesNotExist())
                .andExpect(jsonPath("$.data[0].updatedAt")
                        .doesNotExist());
    }

    @Test
    void requiresLanguageAndDoesNotExposePublicPost() throws Exception {
        mockMvc.perform(get("/api/public/categories"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/public/categories"))
                .andExpect(status().isMethodNotAllowed());
    }
}
