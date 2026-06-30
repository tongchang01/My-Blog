package com.tyb.myblog.v2.stats.web;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.GlobalExceptionHandler;
import com.tyb.myblog.v2.stats.application.StatsDashboardQuery;
import com.tyb.myblog.v2.stats.application.StatsDashboardResult;
import com.tyb.myblog.v2.stats.application.StatsDashboardService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminStatsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminStatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatsDashboardService service;

    private AuthenticatedPrincipal principal;

    @BeforeEach
    void setUp() {
        principal = new AuthenticatedPrincipal(
                "1002", "demo", List.of("DEMO"));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal, null, List.of()));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsDashboardWithUnambiguousUvFields() throws Exception {
        StatsDashboardResult result = new StatsDashboardResult(
                100,
                10,
                8,
                new BigDecimal("5.5"),
                List.of(new StatsDashboardResult.TrendPoint(
                        LocalDate.of(2026, 6, 18), 10, 8)),
                List.of(new StatsDashboardResult.TopArticle(
                        100L, "文章标题", 80, 50)),
                List.of(new StatsDashboardResult.LanguageDistribution(
                        "zh", 100, new BigDecimal("1.0000"))));
        when(service.dashboard(
                principal,
                new StatsDashboardQuery(
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 6, 18))))
                .thenReturn(result);

        mockMvc.perform(get("/api/admin/stats/dashboard")
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-18"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.periodPv").value(100))
                .andExpect(jsonPath("$.data.topArticles[0].articleId")
                        .value("100"))
                .andExpect(jsonPath(
                        "$.data.topArticles[0].dailyUvSum")
                        .value(50))
                .andExpect(jsonPath("$.data.uvTotal").doesNotExist());

        verify(service).dashboard(
                principal,
                new StatsDashboardQuery(
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 6, 18)));
    }
}
