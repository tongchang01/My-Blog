package com.tyb.myblog.v2.stats.web;

import com.tyb.myblog.v2.common.web.ApiResponse;
import com.tyb.myblog.v2.common.web.ClientIpResolver;
import com.tyb.myblog.v2.stats.application.PageViewRecordCommand;
import com.tyb.myblog.v2.stats.application.PageViewRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开页面访问打点接口。
 */
@RestController
@RequestMapping("/api/public/stats/page-views")
@RequiredArgsConstructor
public class PublicPageViewController {

    private final PageViewRecordService service;
    private final ClientIpResolver clientIpResolver;

    @PostMapping
    @Operation(summary = "记录公开页面访问")
    public ApiResponse<Void> record(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(
                            implementation = PageViewRecordOpenApiRequest.class)))
            @Valid @RequestBody PageViewRecordRequest request,
            HttpServletRequest servletRequest) {
        service.record(new PageViewRecordCommand(
                request.articleId(),
                request.lang(),
                clientIpResolver.resolve(servletRequest),
                servletRequest.getHeader("User-Agent"),
                servletRequest.getHeader("Referer")));
        return ApiResponse.ok(null);
    }
}
