package com.aurora.myblog.v2.modules.identity.api;

import com.aurora.myblog.v2.common.web.ApiResponse;
import com.aurora.myblog.v2.modules.identity.application.AuthService;
import com.aurora.myblog.v2.modules.identity.domain.LoginCommand;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var result = authService.login(new LoginCommand(request.username(), request.password()));
        LoginResponse.User user = new LoginResponse.User(
                result.user().id(),
                result.user().username(),
                result.user().roles());
        return ApiResponse.ok(new LoginResponse(result.token().accessToken(), result.token().expiresAt(), user));
    }

    @PostMapping("/logout")
    ApiResponse<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        authService.logout(authorization.replaceFirst("Bearer ", ""));
        return ApiResponse.ok(null);
    }
}
