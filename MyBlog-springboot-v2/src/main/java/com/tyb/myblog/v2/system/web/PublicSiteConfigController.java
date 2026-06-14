package com.tyb.myblog.v2.system.web;

import com.tyb.myblog.v2.common.web.ApiResponse;
import com.tyb.myblog.v2.system.application.siteconfig.PublicSiteConfigQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前台公开站点配置接口。
 */
@Tag(name = "公开站点配置", description = "前台公开站点信息")
@RestController
@RequestMapping("/api/public/site-config")
@RequiredArgsConstructor
public class PublicSiteConfigController {

    private final PublicSiteConfigQueryService queryService;

    /**
     * 查询当前语言的公开站点配置。
     */
    @Operation(summary = "查询当前语言站点配置")
    @GetMapping
    public ApiResponse<PublicSiteConfigVO> get(
            @RequestParam(required = false) String lang) {
        return ApiResponse.ok(
                PublicSiteConfigVO.from(queryService.query(lang)));
    }
}
