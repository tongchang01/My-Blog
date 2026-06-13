package com.tyb.myblog.v2.identity.web;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.auth.CurrentUser;
import com.tyb.myblog.v2.common.web.ApiResponse;
import com.tyb.myblog.v2.common.web.ClientIpResolver;
import com.tyb.myblog.v2.identity.application.auth.AuthApplicationService;
import com.tyb.myblog.v2.identity.application.auth.LoginCommand;
import com.tyb.myblog.v2.identity.application.auth.LoginTokenResult;
import com.tyb.myblog.v2.identity.application.auth.LogoutApplicationService;
import com.tyb.myblog.v2.identity.application.auth.RefreshSessionApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台认证会话接口。
 */
@Tag(name = "后台认证", description = "后台登录、刷新与退出入口")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthApplicationService authApplicationService;
    private final RefreshSessionApplicationService refreshSessionApplicationService;
    private final LogoutApplicationService logoutApplicationService;
    private final ClientIpResolver clientIpResolver;

    /**
     * 使用后台账号密码签发双 token。
     */
    @Operation(summary = "后台登录")
    @PostMapping("/login")
    public ApiResponse<LoginTokenVO> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        LoginTokenResult result =
                authApplicationService.login(new LoginCommand(
                        request.username(),
                        request.password(),
                        clientIpResolver.resolve(servletRequest)));
        return ApiResponse.ok(toTokenView(result));
    }

    /**
     * 轮换 refresh token 并签发新的双 token。
     */
    @Operation(summary = "刷新认证会话")
    @PostMapping("/refresh")
    public ApiResponse<LoginTokenVO> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        LoginTokenResult result =
                refreshSessionApplicationService.refresh(
                        request.refreshToken());
        return ApiResponse.ok(toTokenView(result));
    }

    /**
     * 撤销当前账号在全部设备上的认证会话。
     */
    @Operation(summary = "全端退出")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @CurrentUser AuthenticatedPrincipal principal
    ) {
        logoutApplicationService.logout(principal.id());
        return ApiResponse.ok(null);
    }

    private LoginTokenVO toTokenView(LoginTokenResult result) {
        return new LoginTokenVO(
                result.accessToken(),
                result.refreshToken(),
                result.accessExpiresIn(),
                result.refreshExpiresIn());
    }
}
