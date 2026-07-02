package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.common.error.GlobalExceptionHandler;
import com.tyb.myblog.v2.content.application.article.PublicArticleDetailResult;
import com.tyb.myblog.v2.content.application.article.PublicArticleHomeResult;
import com.tyb.myblog.v2.content.application.article.PublicArticlePageResult;
import com.tyb.myblog.v2.content.application.article.PublicArticleQuery;
import com.tyb.myblog.v2.content.application.article.PublicArticleQueryService;
import com.tyb.myblog.v2.content.application.article.PublicArticleTagResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicArticleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        GlobalExceptionHandler.class,
        ArticleWebMapping.class
})
class PublicArticleControllerTest {

    private static final long ARTICLE_ID = 9007199254740993L;
    private static final long CATEGORY_ID = 9007199254740995L;
    private static final long TAG_ID = 9007199254740997L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublicArticleQueryService queryService;

    @Test
    void returnsPublicPageWithoutBodyAndWithLockedFlag()
            throws Exception {
        when(queryService.page(new PublicArticleQuery(
                1, 20, "ja", 10L, 20L, "Spring", "2026-06")))
                .thenReturn(new PublicArticlePageResult(
                        List.of(pageItem()), 1, 1, 20));

        mockMvc.perform(get("/api/public/articles")
                        .queryParam("lang", "ja")
                        .queryParam("categoryId", "10")
                        .queryParam("tagId", "20")
                        .queryParam("keyword", "Spring")
                        .queryParam("archiveMonth", "2026-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].id")
                        .value(Long.toString(ARTICLE_ID)))
                .andExpect(jsonPath("$.data.records[0].categoryId")
                        .value(Long.toString(CATEGORY_ID)))
                .andExpect(jsonPath("$.data.records[0].tags[0].id")
                        .value(Long.toString(TAG_ID)))
                .andExpect(jsonPath("$.data.records[0].title")
                        .value("标题"))
                .andExpect(jsonPath("$.data.records[0].locked")
                        .value(true))
                .andExpect(jsonPath("$.data.records[0].status")
                        .doesNotExist())
                .andExpect(jsonPath("$.data.records[0].coverAttachmentId")
                        .doesNotExist())
                .andExpect(jsonPath("$.data.records[0].body")
                        .doesNotExist());
    }

    @Test
    void returnsPublicHomeArticles() throws Exception {
        when(queryService.home("en", 10))
                .thenReturn(new PublicArticleHomeResult(
                        pageItem(),
                        List.of(pageItem()),
                        List.of(pageItem())));

        mockMvc.perform(get("/api/public/articles/home")
                        .queryParam("lang", "en")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pinnedArticle.id")
                        .value(Long.toString(ARTICLE_ID)))
                .andExpect(jsonPath("$.data.featuredArticles[0].id")
                        .value(Long.toString(ARTICLE_ID)))
                .andExpect(jsonPath("$.data.articles[0].id")
                        .value(Long.toString(ARTICLE_ID)))
                .andExpect(jsonPath("$.data.pinnedArticle.body")
                        .doesNotExist());
    }

    @Test
    void returnsPublishedDetailAndMapsPasswordToForbidden()
            throws Exception {
        when(queryService.detail(100L, "zh"))
                .thenReturn(detail());
        when(queryService.detail(101L, "zh"))
                .thenThrow(new ApiException(ApiErrorCode.FORBIDDEN));

        mockMvc.perform(get("/api/public/articles/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id")
                        .value(Long.toString(ARTICLE_ID)))
                .andExpect(jsonPath("$.data.categoryId")
                        .value(Long.toString(CATEGORY_ID)))
                .andExpect(jsonPath("$.data.tags[0].id")
                        .value(Long.toString(TAG_ID)))
                .andExpect(jsonPath("$.data.body").value("正文"))
                .andExpect(jsonPath("$.data.status").doesNotExist())
                .andExpect(jsonPath("$.data.coverAttachmentId")
                        .doesNotExist())
                .andExpect(jsonPath("$.data.updatedAt")
                        .value("2026-06-15T11:00:00"))
                .andExpect(jsonPath("$.data.locked").value(false));
        mockMvc.perform(get("/api/public/articles/101"))
                .andExpect(status().isForbidden());

        verify(queryService).detail(100L, "zh");
    }

    private PublicArticlePageResult.Item pageItem() {
        return new PublicArticlePageResult.Item(
                ARTICLE_ID,
                "标题",
                "摘要",
                CATEGORY_ID,
                "分类",
                "article",
                LocalDateTime.of(2026, 6, 15, 10, 0),
                "https://cdn.example.com/c.png",
                2,
                List.of(new PublicArticleTagResult(
                        TAG_ID, "标签", "tag")),
                LocalDateTime.of(2026, 6, 15, 9, 0),
                true);
    }

    private PublicArticleDetailResult detail() {
        return new PublicArticleDetailResult(
                ARTICLE_ID,
                "标题",
                "摘要",
                "正文",
                CATEGORY_ID,
                "分类",
                "article",
                LocalDateTime.of(2026, 6, 15, 10, 0),
                "https://cdn.example.com/c.png",
                2,
                List.of(new PublicArticleTagResult(
                        TAG_ID, "标签", "tag")),
                LocalDateTime.of(2026, 6, 15, 9, 0),
                LocalDateTime.of(2026, 6, 15, 11, 0),
                false);
    }
}
