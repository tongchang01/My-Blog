package com.tyb.myblog.v2.stats.web;

import com.tyb.myblog.v2.common.error.GlobalExceptionHandler;
import com.tyb.myblog.v2.common.web.ClientIpResolver;
import com.tyb.myblog.v2.stats.application.PageViewRecordCommand;
import com.tyb.myblog.v2.stats.application.PageViewRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicPageViewController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PublicPageViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PageViewRecordService service;

    @MockitoBean
    private ClientIpResolver clientIpResolver;

    @Test
    void recordsHeadersAndTrustedClientAddress() throws Exception {
        when(clientIpResolver.resolve(any()))
                .thenReturn("203.0.113.1");

        mockMvc.perform(post("/api/public/stats/page-views")
                        .header("User-Agent", "JUnit")
                        .header("Referer", "https://example.com/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"articleId":"100","lang":"zh"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));

        verify(service).record(new PageViewRecordCommand(
                100L,
                "zh",
                "203.0.113.1",
                "JUnit",
                "https://example.com/posts"));
    }

    @Test
    void rejectsMissingLanguage() throws Exception {
        mockMvc.perform(post("/api/public/stats/page-views")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("90001"));
    }
}
