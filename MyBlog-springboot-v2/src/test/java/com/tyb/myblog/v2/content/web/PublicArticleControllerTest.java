package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.common.error.GlobalExceptionHandler;
import com.tyb.myblog.v2.content.application.article.PublicArticleDetailResult;
import com.tyb.myblog.v2.content.application.article.PublicArticlePageResult;
import com.tyb.myblog.v2.content.application.article.PublicArticleQuery;
import com.tyb.myblog.v2.content.application.article.PublicArticleQueryService;
import com.tyb.myblog.v2.content.application.article.PublicArticleTagResult;
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
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
                .andExpect(jsonPath("$.data.records[0].title")
                        .value("标题"))
                .andExpect(jsonPath("$.data.records[0].locked")
                        .value(true))
                .andExpect(jsonPath("$.data.records[0].body")
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
                .andExpect(jsonPath("$.data.body").value("正文"))
                .andExpect(jsonPath("$.data.locked").value(false));
        mockMvc.perform(get("/api/public/articles/101"))
                .andExpect(status().isForbidden());

        verify(queryService).detail(100L, "zh");
    }

    private PublicArticlePageResult.Item pageItem() {
        return new PublicArticlePageResult.Item(
                100L,
                "标题",
                "摘要",
                10L,
                "分类",
                "article",
                ArticleStatus.PASSWORD,
                LocalDateTime.of(2026, 6, 15, 10, 0),
                300L,
                "https://cdn.example.com/c.png",
                2,
                List.of(new PublicArticleTagResult(
                        20L, "标签", "tag")),
                LocalDateTime.of(2026, 6, 15, 9, 0),
                true);
    }

    private PublicArticleDetailResult detail() {
        return new PublicArticleDetailResult(
                100L,
                "标题",
                "摘要",
                "正文",
                10L,
                "分类",
                "article",
                ArticleStatus.PUBLISHED,
                LocalDateTime.of(2026, 6, 15, 10, 0),
                300L,
                "https://cdn.example.com/c.png",
                2,
                List.of(new PublicArticleTagResult(
                        20L, "标签", "tag")),
                LocalDateTime.of(2026, 6, 15, 9, 0),
                false);
    }
}
