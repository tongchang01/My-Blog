package com.tyb.myblog.v2.stats.web;

import com.tyb.myblog.v2.common.web.ApiResponse;
import com.tyb.myblog.v2.stats.application.SiteStatsSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 公开访问统计读取接口。 */
@RestController
@RequestMapping("/api/public/stats")
@RequiredArgsConstructor
public class PublicStatsController {

    private final SiteStatsSummaryService service;

    @GetMapping("/site-summary")
    @Operation(summary = "查询公开站点统计摘要")
    public ApiResponse<SiteStatsSummaryVO> siteSummary() {
        return ApiResponse.ok(SiteStatsSummaryVO.from(service.summary()));
    }
}
