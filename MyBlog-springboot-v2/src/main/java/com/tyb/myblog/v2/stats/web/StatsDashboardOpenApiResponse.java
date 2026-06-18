package com.tyb.myblog.v2.stats.web;

import io.swagger.v3.oas.annotations.media.Schema;

/** 后台访问统计总览的 OpenAPI 成功响应模型。 */
@Schema(name = "StatsDashboardResponse")
public record StatsDashboardOpenApiResponse(
        String code,
        String msg,
        StatsDashboardVO data) {
}
