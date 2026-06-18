package com.tyb.myblog.v2.stats.web;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.auth.CurrentUser;
import com.tyb.myblog.v2.common.web.ApiResponse;
import com.tyb.myblog.v2.stats.application.StatsDashboardQuery;
import com.tyb.myblog.v2.stats.application.StatsDashboardResult;
import com.tyb.myblog.v2.stats.application.StatsDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** 后台访问统计总览接口。 */
@Tag(name = "后台访问统计", description = "后台访问趋势和分布查询")
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final StatsDashboardService service;

    @Operation(summary = "查询访问统计总览")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            content = @Content(schema = @Schema(
                    implementation = StatsDashboardOpenApiResponse.class)))
    @GetMapping("/dashboard")
    public ApiResponse<StatsDashboardVO> dashboard(
            @CurrentUser AuthenticatedPrincipal principal,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to) {
        StatsDashboardResult result = service.dashboard(
                principal,
                new StatsDashboardQuery(from, to));
        return ApiResponse.ok(StatsDashboardVO.from(result));
    }
}
