package com.tyb.myblog.v2.identity.web;

import com.tyb.myblog.v2.common.web.ApiResponse;
import com.tyb.myblog.v2.common.web.ClientIpResolver;
import com.tyb.myblog.v2.identity.application.auth.AuthApplicationService;
import com.tyb.myblog.v2.identity.application.auth.LoginCommand;
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
 * 后台认证接口。
 */
@Tag(name = "后台认证", description = "后台登录与会话入口")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthApplicationService authApplicationService;
    private final ClientIpResolver clientIpResolver;

    /**
     * 使用后台账号密码签发双 token。
     *
     * @param request 登录请求
     * @param servletRequest 当前 HTTP 请求
     * @return 双 token 响应
     */
    @Operation(summary = "后台登录")
    @PostMapping("/login")
    public ApiResponse<LoginTokenVO> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        var result = authApplicationService.login(new LoginCommand(
                request.username(),
                request.password(),
                clientIpResolver.resolve(servletRequest)));
        return ApiResponse.ok(new LoginTokenVO(
                result.accessToken(),
                result.refreshToken(),
                result.accessExpiresIn(),
                result.refreshExpiresIn()));
    }
}
