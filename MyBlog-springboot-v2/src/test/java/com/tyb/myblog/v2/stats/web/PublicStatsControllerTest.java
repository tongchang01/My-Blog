package com.tyb.myblog.v2.stats.web;

import com.tyb.myblog.v2.common.error.GlobalExceptionHandler;
import com.tyb.myblog.v2.stats.application.SiteStatsSummaryResult;
import com.tyb.myblog.v2.stats.application.SiteStatsSummaryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicStatsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PublicStatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SiteStatsSummaryService service;

    @Test
    void returnsPublicSiteSummary() throws Exception {
        when(service.summary()).thenReturn(new SiteStatsSummaryResult(7, 42));

        mockMvc.perform(get("/api/public/stats/site-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.todayUv").value(7))
                .andExpect(jsonPath("$.data.totalPv").value(42));
    }
}
