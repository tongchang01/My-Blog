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

/**
 * 登录认证应用服务。
 *
 * <p>负责编排账号读取、密码校验、登录审计和访问令牌签发。
 * 该类不直接读取数据库表，账号来源由 {@link UserCredentialReader} 实现层适配。</p>
 */
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

    /**
     * 执行用户名密码登录。
     *
     * <p>登录成功后会记录最后登录时间和客户端 IP，并签发访问令牌。
     * 登录失败统一返回凭证错误，避免暴露账号是否存在。</p>
     *
     * @param command 登录命令
     * @return 登录用户和访问令牌
     */
    public LoginResult login(LoginCommand command) {
        var credential = credentialReader.findByUsername(command.username())
                .filter(user -> passwordEncoder.matches(command.password(), user.passwordHash()))
                .orElseThrow(() -> new ApiException(ApiErrorCode.BAD_CREDENTIALS, "用户名或密码错误"));
        AuthenticatedUser user = new AuthenticatedUser(
                credential.id(),
                credential.username(),
                Set.copyOf(credential.roles()));
        if (auditRecorder != null) {
            // 登录审计失败不应由此处吞掉，避免线上长期丢失安全审计数据。
            auditRecorder.recordSuccessfulLogin(credential.id(), command.clientIp());
        }
        AuthTokenService.TokenIssueResult token = tokenService.issueAccessToken(user);
        return new LoginResult(user, token);
    }

    /**
     * 注销当前访问令牌。
     *
     * @param accessToken 原始访问令牌
     */
    public void logout(String accessToken) {
        tokenService.revoke(accessToken);
    }

    /**
     * 登录成功后的应用层结果。
     *
     * @param user  已认证用户
     * @param token 访问令牌签发结果
     */
    public record LoginResult(AuthenticatedUser user, AuthTokenService.TokenIssueResult token) {
    }
}
