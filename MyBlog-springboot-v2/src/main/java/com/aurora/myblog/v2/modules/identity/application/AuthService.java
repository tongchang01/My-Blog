package com.aurora.myblog.v2.modules.identity.application;

import com.aurora.myblog.v2.common.error.ApiErrorCode;
import com.aurora.myblog.v2.common.error.ApiException;
import com.aurora.myblog.v2.modules.identity.domain.AuthenticatedUser;
import com.aurora.myblog.v2.modules.identity.domain.LoginCommand;
import com.aurora.myblog.v2.modules.identity.domain.UserCredentialReader;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthService {

    private final UserCredentialReader credentialReader;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService tokenService;

    public AuthService(UserCredentialReader credentialReader, PasswordEncoder passwordEncoder, AuthTokenService tokenService) {
        this.credentialReader = credentialReader;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public LoginResult login(LoginCommand command) {
        var credential = credentialReader.findByUsername(command.username())
                .filter(user -> passwordEncoder.matches(command.password(), user.passwordHash()))
                .orElseThrow(() -> new ApiException(ApiErrorCode.BAD_CREDENTIALS, "用户名或密码错误"));
        AuthenticatedUser user = new AuthenticatedUser(
                credential.id(),
                credential.username(),
                Set.copyOf(credential.roles()));
        AuthTokenService.TokenIssueResult token = tokenService.issueAccessToken(user);
        return new LoginResult(user, token);
    }

    public void logout(String accessToken) {
        tokenService.revoke(accessToken);
    }

    public record LoginResult(AuthenticatedUser user, AuthTokenService.TokenIssueResult token) {
    }
}
