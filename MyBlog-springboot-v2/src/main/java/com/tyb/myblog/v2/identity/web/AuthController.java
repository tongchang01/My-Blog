package com.tyb.myblog.v2.identity.web;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.auth.CurrentUser;
import com.tyb.myblog.v2.common.web.ApiResponse;
import com.tyb.myblog.v2.common.web.ClientIpResolver;
import com.tyb.myblog.v2.identity.application.AuthService;
import com.tyb.myblog.v2.identity.application.IdentityQueryService;
import com.tyb.myblog.v2.identity.domain.AuthRole;
import com.tyb.myblog.v2.identity.domain.LoginCommand;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
/**
 * 认证接口。
 *
 * <p>负责登录、查询当前用户和注销。登录接口会记录客户端 IP，
 * 当前用户接口依赖 JWT 认证上下文。</p>
 */
public class AuthController {

    private final AuthService authService;
    private final IdentityQueryService identityQueryService;

    public AuthController(AuthService authService, IdentityQueryService identityQueryService) {
        this.authService = authService;
        this.identityQueryService = identityQueryService;
    }

    /**
     * 用户名密码登录。
     *
     * <p>登录成功后返回访问令牌和最小用户信息，失败时不区分账号不存在和密码错误。</p>
     */
    @PostMapping("/login")
    ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        var result = authService.login(new LoginCommand(
                request.username(),
                request.password(),
                ClientIpResolver.resolve(servletRequest)));
        LoginResponse.User user = new LoginResponse.User(
                result.user().id(),
                result.user().username(),
                result.user().roles());
        return ApiResponse.ok(new LoginResponse(result.token().accessToken(), result.token().expiresAt(), user));
    }

    /**
     * 查询当前登录用户资料。
     */
    @GetMapping("/me")
    ApiResponse<MeResponse> me(@CurrentUser AuthenticatedPrincipal user) {
        var profile = identityQueryService.currentProfile(user);
        return ApiResponse.ok(new MeResponse(
                profile.authId(),
                profile.userInfoId(),
                profile.username(),
                profile.nickname(),
                profile.avatar(),
                profile.email(),
                user.roles().stream().map(AuthRole::valueOf).collect(java.util.stream.Collectors.toSet())));
    }

    /**
     * 注销当前访问令牌。
     */
    @PostMapping("/logout")
    ApiResponse<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        authService.logout(authorization.replaceFirst("Bearer ", ""));
        return ApiResponse.ok(null);
    }
}
