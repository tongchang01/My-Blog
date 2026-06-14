package com.tyb.myblog.v2.system.web;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.auth.CurrentUser;
import com.tyb.myblog.v2.common.web.ApiResponse;
import com.tyb.myblog.v2.system.application.siteconfig.AdminSiteConfigQueryService;
import com.tyb.myblog.v2.system.application.siteconfig.SiteConfigUpdateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final SiteConfigUpdateService updateService;

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

    /**
     * 由 ADMIN 全量更新站点配置。
     */
    @Operation(
            summary = "全量更新站点配置",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(schema = @Schema(
                            implementation =
                                    UpdateSiteConfigOpenApiRequest.class))))
    @PutMapping
    public ApiResponse<AdminSiteConfigVO> update(
            @CurrentUser AuthenticatedPrincipal principal,
            @RequestBody UpdateSiteConfigRequest request) {
        return ApiResponse.ok(AdminSiteConfigVO.from(
                updateService.update(principal, request.toCommand())));
    }
}
