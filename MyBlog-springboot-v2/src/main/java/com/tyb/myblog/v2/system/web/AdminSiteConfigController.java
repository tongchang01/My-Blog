package com.tyb.myblog.v2.system.web;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.auth.CurrentUser;
import com.tyb.myblog.v2.common.web.ApiResponse;
import com.tyb.myblog.v2.system.application.siteconfig.AdminSiteConfigQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台站点配置读取与维护接口。
 */
@Tag(name = "后台站点配置", description = "站点配置读取与维护")
@RestController
@RequestMapping("/api/admin/site-config")
@RequiredArgsConstructor
public class AdminSiteConfigController {

    private final AdminSiteConfigQueryService queryService;

    /**
     * 查询完整站点配置。
     */
    @Operation(summary = "查询完整站点配置")
    @GetMapping
    public ApiResponse<AdminSiteConfigVO> get(
            @CurrentUser AuthenticatedPrincipal principal) {
        return ApiResponse.ok(
                AdminSiteConfigVO.from(queryService.query(principal)));
    }
}
