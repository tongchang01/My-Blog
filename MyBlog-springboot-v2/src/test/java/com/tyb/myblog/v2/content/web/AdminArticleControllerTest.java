package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.common.error.GlobalExceptionHandler;
import com.tyb.myblog.v2.content.application.article.AdminArticleDetailResult;
import com.tyb.myblog.v2.content.application.article.AdminArticlePageResult;
import com.tyb.myblog.v2.content.application.article.ArticleCreateService;
import com.tyb.myblog.v2.content.application.article.ArticleDeleteService;
import com.tyb.myblog.v2.content.application.article.ArticleQueryService;
import com.tyb.myblog.v2.content.application.article.ArticleRestoreService;
import com.tyb.myblog.v2.content.application.article.ArticleResult;
import com.tyb.myblog.v2.content.application.article.ArticleUpdateService;
import com.tyb.myblog.v2.content.application.article.CreateArticleCommand;
import com.tyb.myblog.v2.content.application.article.DeletedArticlePageResult;
import com.tyb.myblog.v2.content.application.article.DeletedArticleQueryService;
import com.tyb.myblog.v2.content.application.article.UpdateArticleCommand;
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminArticleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        GlobalExceptionHandler.class,
        ArticleWebMapping.class
})
class AdminArticleControllerTest {

    private static final long UNSAFE_BROWSER_ID = 9_007_199_254_740_993L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArticleQueryService queryService;

    @MockitoBean
    private ArticleCreateService createService;

    @MockitoBean
    private ArticleUpdateService updateService;

    @MockitoBean
    private ArticleDeleteService deleteService;

    @MockitoBean
    private ArticleRestoreService restoreService;

    @MockitoBean
    private DeletedArticleQueryService deletedQueryService;

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
    void returnsAdminPageAndDetailWithoutPasswordFields()
            throws Exception {
        when(queryService.adminPage(any(), any()))
                .thenReturn(new AdminArticlePageResult(
                        List.of(pageItem()), 1, 1, 20));
        when(queryService.adminDetail(principal, 100L))
                .thenReturn(detail());

        mockMvc.perform(get("/api/admin/articles")
                        .queryParam("status", "PUBLISHED")
                        .queryParam("page", "1")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].id")
                        .value(Long.toString(UNSAFE_BROWSER_ID)))
                .andExpect(jsonPath("$.data.records[0].categoryId")
                        .value(Long.toString(UNSAFE_BROWSER_ID)))
                .andExpect(jsonPath("$.data.records[0].coverAttachmentId")
                        .value(Long.toString(UNSAFE_BROWSER_ID)))
                .andExpect(jsonPath("$.data.records[0].tagIds[0]")
                        .value(Long.toString(UNSAFE_BROWSER_ID)))
                .andExpect(jsonPath("$.data.records[0].createdBy")
                        .value(Long.toString(UNSAFE_BROWSER_ID)))
                .andExpect(jsonPath("$.data.records[0].updatedBy")
                        .value(Long.toString(UNSAFE_BROWSER_ID)))
                .andExpect(jsonPath("$.data.records[0].titleZh")
                        .value("标题"))
                .andExpect(jsonPath("$.data.records[0].coverUrl")
                        .value("https://cdn.example.com/c.png"))
                .andExpect(jsonPath("$.data.records[0].accessPassword")
                        .doesNotExist());

        mockMvc.perform(get("/api/admin/articles/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id")
                        .value(Long.toString(UNSAFE_BROWSER_ID)))
                .andExpect(jsonPath("$.data.categoryId")
                        .value(Long.toString(UNSAFE_BROWSER_ID)))
                .andExpect(jsonPath("$.data.authorId")
                        .value(Long.toString(UNSAFE_BROWSER_ID)))
                .andExpect(jsonPath("$.data.coverAttachmentId")
                        .value(Long.toString(UNSAFE_BROWSER_ID)))
                .andExpect(jsonPath("$.data.tagIds[0]")
                        .value(Long.toString(UNSAFE_BROWSER_ID)))
                .andExpect(jsonPath("$.data.createdBy")
                        .value(Long.toString(UNSAFE_BROWSER_ID)))
                .andExpect(jsonPath("$.data.updatedBy")
                        .value(Long.toString(UNSAFE_BROWSER_ID)))
                .andExpect(jsonPath("$.data.body").value("正文"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.accessPassword").doesNotExist());
    }

    @Test
    void createsAndUpdatesWithCompleteSubmittedFields()
            throws Exception {
        when(createService.create(any(), any()))
                .thenReturn(articleResult());
        when(updateService.update(any(), any(Long.class), any()))
                .thenReturn(articleResult());
        when(queryService.adminDetail(principal, 100L))
                .thenReturn(detail());

        mockMvc.perform(post("/api/admin/articles")
                        .contentType("application/json")
                        .content(writeBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id")
                        .value(Long.toString(UNSAFE_BROWSER_ID)));
        mockMvc.perform(put("/api/admin/articles/100")
                        .contentType("application/json")
                        .content(writeBody()))
                .andExpect(status().isOk());

        verify(createService).create(
                principal,
                new CreateArticleCommand(
                        "标题",
                        null,
                        "Title",
                        "摘要",
                        null,
                        null,
                        "正文",
                        10L,
                        List.of(20L),
                        "article",
                        ArticleStatus.PUBLISHED,
                        null,
                        null,
                        300L));
        verify(updateService).update(
                principal,
                100L,
                new UpdateArticleCommand(
                        "标题",
                        null,
                        "Title",
                        "摘要",
                        null,
                        null,
                        "正文",
                        10L,
                        List.of(20L),
                        "article",
                        ArticleStatus.PUBLISHED,
                        null,
                        null,
                        300L));
    }

    @Test
    void rejectsMissingUnknownAndInvalidWriteFields()
            throws Exception {
        mockMvc.perform(post("/api/admin/articles")
                        .contentType("application/json")
                        .content("""
                                {
                                  "titleZh":"标题"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("90001"));
        mockMvc.perform(post("/api/admin/articles")
                        .contentType("application/json")
                        .content(writeBody().replace(
                                "\"coverAttachmentId\":300",
                                "\"coverAttachmentId\":300,\"unknown\":true")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/admin/articles")
                        .contentType("application/json")
                        .content(writeBody().replace(
                                "\"status\":\"PUBLISHED\"",
                                "\"status\":\"INVALID\"")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mapsDemoWriteRejectionToForbidden() throws Exception {
        when(createService.create(any(), any()))
                .thenThrow(new ApiException(ApiErrorCode.FORBIDDEN));

        mockMvc.perform(post("/api/admin/articles")
                        .contentType("application/json")
                        .content(writeBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("10003"));
    }

    @Test
    void deletesRestoresAndReadsRecycleBin() throws Exception {
        doNothing().when(deleteService).delete(principal, 100L);
        when(deletedQueryService.page(principal, 1, 20))
                .thenReturn(new DeletedArticlePageResult(
                        List.of(new DeletedArticlePageResult.Item(
                                UNSAFE_BROWSER_ID,
                                "标题",
                                null,
                                null,
                                ArticleStatus.PUBLISHED,
                                UNSAFE_BROWSER_ID - 1,
                                LocalDateTime.of(2026, 6, 16, 12, 0),
                                UNSAFE_BROWSER_ID - 2)),
                        1,
                        1,
                        20));
        when(restoreService.restore(principal, 100L))
                .thenReturn(articleResult());
        when(queryService.adminDetail(principal, 100L))
                .thenReturn(detail());

        mockMvc.perform(delete("/api/admin/articles/100"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/articles/recycle-bin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].id")
                        .value(Long.toString(UNSAFE_BROWSER_ID)))
                .andExpect(jsonPath("$.data.records[0].categoryId")
                        .value(Long.toString(UNSAFE_BROWSER_ID - 1)))
                .andExpect(jsonPath("$.data.records[0].deletedBy")
                        .value(Long.toString(UNSAFE_BROWSER_ID - 2)));
        mockMvc.perform(post("/api/admin/articles/100/restore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id")
                        .value(Long.toString(UNSAFE_BROWSER_ID)));

        verify(deleteService).delete(principal, 100L);
        verify(restoreService).restore(principal, 100L);
    }

    private String writeBody() {
        return """
                {
                  "titleZh":"标题",
                  "titleJa":null,
                  "titleEn":"Title",
                  "summaryZh":"摘要",
                  "summaryJa":null,
                  "summaryEn":null,
                  "body":"正文",
                  "categoryId":10,
                  "tagIds":[20],
                  "slug":"article",
                  "status":"PUBLISHED",
                  "password":null,
                  "publishAt":null,
                  "coverAttachmentId":300
                }
                """;
    }

    private AdminArticlePageResult.Item pageItem() {
        return new AdminArticlePageResult.Item(
                UNSAFE_BROWSER_ID,
                "标题",
                null,
                "Title",
                "摘要",
                null,
                null,
                UNSAFE_BROWSER_ID,
                "分类",
                "article",
                ArticleStatus.PUBLISHED,
                LocalDateTime.of(2026, 6, 15, 10, 0),
                UNSAFE_BROWSER_ID,
                "https://cdn.example.com/c.png",
                2,
                List.of(UNSAFE_BROWSER_ID),
                LocalDateTime.of(2026, 6, 15, 9, 0),
                UNSAFE_BROWSER_ID,
                LocalDateTime.of(2026, 6, 15, 11, 0),
                UNSAFE_BROWSER_ID);
    }

    private AdminArticleDetailResult detail() {
        return new AdminArticleDetailResult(
                UNSAFE_BROWSER_ID,
                "标题",
                null,
                "Title",
                "摘要",
                null,
                null,
                "正文",
                UNSAFE_BROWSER_ID,
                "分类",
                UNSAFE_BROWSER_ID,
                "article",
                ArticleStatus.PUBLISHED,
                LocalDateTime.of(2026, 6, 15, 10, 0),
                UNSAFE_BROWSER_ID,
                "https://cdn.example.com/c.png",
                2,
                List.of(UNSAFE_BROWSER_ID),
                LocalDateTime.of(2026, 6, 15, 9, 0),
                UNSAFE_BROWSER_ID,
                LocalDateTime.of(2026, 6, 15, 11, 0),
                UNSAFE_BROWSER_ID);
    }

    private ArticleResult articleResult() {
        return new ArticleResult(
                100L,
                "标题",
                null,
                "Title",
                "摘要",
                null,
                null,
                "正文",
                10L,
                1001L,
                "article",
                ArticleStatus.PUBLISHED,
                LocalDateTime.of(2026, 6, 15, 10, 0),
                300L,
                2,
                List.of(20L),
                LocalDateTime.of(2026, 6, 15, 9, 0),
                1001L,
                LocalDateTime.of(2026, 6, 15, 11, 0),
                1001L);
    }
}
