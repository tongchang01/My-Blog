package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.common.error.GlobalExceptionHandler;
import com.tyb.myblog.v2.content.application.article.PublicArchivePageResult;
import com.tyb.myblog.v2.content.application.article.PublicArticleQuery;
import com.tyb.myblog.v2.content.application.article.PublicArticleQueryService;
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

@WebMvcTest(PublicArchiveController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        GlobalExceptionHandler.class,
        ArticleWebMapping.class
})
class PublicArchiveControllerTest {

    private static final long ARTICLE_ID = 9007199254740993L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublicArticleQueryService queryService;

    @Test
    void returnsPublicArchiveTimelineWithoutInternalArticleFields()
            throws Exception {
        PublicArticleQuery query = new PublicArticleQuery(
                2, 12, "en", null, null, null, null, null, null);
        when(queryService.archives(query))
                .thenReturn(new PublicArchivePageResult(
                        List.of(new PublicArchivePageResult.Group(
                                "2026-06",
                                2026,
                                6,
                                List.of(new PublicArchivePageResult.Item(
                                        ARTICLE_ID,
                                        "Title",
                                        "article",
                                        LocalDateTime.of(2026, 6, 15, 10, 0),
                                        "Summary")))),
                        20,
                        2,
                        12));

        mockMvc.perform(get("/api/public/archives")
                        .queryParam("page", "2")
                        .queryParam("size", "12")
                        .queryParam("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(20))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.size").value(12))
                .andExpect(jsonPath("$.data.records[0].yearMonth")
                        .value("2026-06"))
                .andExpect(jsonPath("$.data.records[0].year").value(2026))
                .andExpect(jsonPath("$.data.records[0].month").value(6))
                .andExpect(jsonPath("$.data.records[0].articles[0].id")
                        .value(Long.toString(ARTICLE_ID)))
                .andExpect(jsonPath("$.data.records[0].articles[0].title")
                        .value("Title"))
                .andExpect(jsonPath("$.data.records[0].articles[0].slug")
                        .value("article"))
                .andExpect(jsonPath("$.data.records[0].articles[0].publishedAt")
                        .value("2026-06-15T10:00:00"))
                .andExpect(jsonPath("$.data.records[0].articles[0].summary")
                        .value("Summary"))
                .andExpect(jsonPath("$.data.records[0].articles[0].body")
                        .doesNotExist())
                .andExpect(jsonPath("$.data.records[0].articles[0].status")
                        .doesNotExist())
                .andExpect(jsonPath("$.data.records[0].articles[0].coverUrl")
                        .doesNotExist())
                .andExpect(jsonPath("$.data.records[0].articles[0].tags")
                        .doesNotExist());

        verify(queryService).archives(query);
    }
}
