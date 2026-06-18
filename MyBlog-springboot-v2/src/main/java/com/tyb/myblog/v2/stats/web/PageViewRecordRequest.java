package com.tyb.myblog.v2.stats.web;

import jakarta.validation.constraints.NotBlank;

/**
 * 公开页面访问打点请求。
 */
public record PageViewRecordRequest(
        Long articleId,
        @NotBlank(message = "语言不能为空") String lang) {
}
