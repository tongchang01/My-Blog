package com.aurora.myblog.v2.modules.identity.api;

import com.aurora.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.aurora.myblog.v2.common.auth.CurrentUser;
import com.aurora.myblog.v2.common.web.ApiResponse;
import com.aurora.myblog.v2.common.web.ClientIpResolver;
import com.aurora.myblog.v2.modules.identity.application.AuthService;
import com.aurora.myblog.v2.modules.identity.application.IdentityQueryService;
import com.aurora.myblog.v2.modules.identity.domain.AuthRole;
import com.aurora.myblog.v2.modules.identity.domain.LoginCommand;
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
public class AuthController {

    private final AuthService authService;
    private final IdentityQueryService identityQueryService;

    public AuthController(AuthService authService, IdentityQueryService identityQueryService) {
        this.authService = authService;
        this.identityQueryService = identityQueryService;
    }

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

    @PostMapping("/logout")
    ApiResponse<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        authService.logout(authorization.replaceFirst("Bearer ", ""));
        return ApiResponse.ok(null);
    }
}
