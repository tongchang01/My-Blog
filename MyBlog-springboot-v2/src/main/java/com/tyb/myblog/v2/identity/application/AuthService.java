package com.tyb.myblog.v2.identity.application;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.identity.domain.AuthenticatedUser;
import com.tyb.myblog.v2.identity.domain.LoginAuditRecorder;
import com.tyb.myblog.v2.identity.domain.LoginCommand;
import com.tyb.myblog.v2.identity.domain.UserCredentialReader;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthService {

    private final UserCredentialReader credentialReader;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService tokenService;
    private final LoginAuditRecorder auditRecorder;

    public AuthService(
            UserCredentialReader credentialReader,
            PasswordEncoder passwordEncoder,
            AuthTokenService tokenService,
            @Nullable
            LoginAuditRecorder auditRecorder) {
        this.credentialReader = credentialReader;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.auditRecorder = auditRecorder;
    }

    public LoginResult login(LoginCommand command) {
        var credential = credentialReader.findByUsername(command.username())
                .filter(user -> passwordEncoder.matches(command.password(), user.passwordHash()))
                .orElseThrow(() -> new ApiException(ApiErrorCode.BAD_CREDENTIALS, "用户名或密码错误"));
        AuthenticatedUser user = new AuthenticatedUser(
                credential.id(),
                credential.username(),
                Set.copyOf(credential.roles()));
        if (auditRecorder != null) {
            auditRecorder.recordSuccessfulLogin(credential.id(), command.clientIp());
        }
        AuthTokenService.TokenIssueResult token = tokenService.issueAccessToken(user);
        return new LoginResult(user, token);
    }

    public void logout(String accessToken) {
        tokenService.revoke(accessToken);
    }

    public record LoginResult(AuthenticatedUser user, AuthTokenService.TokenIssueResult token) {
    }
}
