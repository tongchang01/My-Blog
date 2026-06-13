package com.tyb.myblog.v2.identity.web;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.auth.CurrentUser;
import com.tyb.myblog.v2.common.web.ApiResponse;
import com.tyb.myblog.v2.identity.application.profile.CurrentUserProfileQueryService;
import com.tyb.myblog.v2.identity.application.profile.CurrentUserProfileUpdateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前登录账号与个人资料接口。
 */
@Tag(name = "当前用户", description = "当前后台账号与个人资料")
@RestController
@RequestMapping("/api/auth/me")
@RequiredArgsConstructor
public class CurrentUserController {

    private final CurrentUserProfileQueryService queryService;
    private final CurrentUserProfileUpdateService updateService;

    /**
     * 查询当前登录账号及完整个人资料。
     */
    @Operation(summary = "查询当前用户资料")
    @GetMapping
    public ApiResponse<CurrentUserVO> get(
            @CurrentUser AuthenticatedPrincipal principal
    ) {
        return ApiResponse.ok(
                CurrentUserVO.from(queryService.query(principal.id())));
    }

    /**
     * 部分更新当前 ADMIN 账号的个人资料。
     */
    @Operation(
            summary = "更新当前用户资料",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(schema = @Schema(
                            implementation =
                                    UpdateCurrentUserProfileOpenApiRequest.class))))
    @PatchMapping("/profile")
    public ApiResponse<UserProfileVO> update(
            @CurrentUser AuthenticatedPrincipal principal,
            @RequestBody UpdateCurrentUserProfileRequest request
    ) {
        return ApiResponse.ok(UserProfileVO.from(
                updateService.update(principal, request.toCommand())));
    }
}
