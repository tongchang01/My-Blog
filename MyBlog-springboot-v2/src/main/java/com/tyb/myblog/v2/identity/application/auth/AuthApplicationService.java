package com.tyb.myblog.v2.identity.application.auth;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.identity.domain.auth.LoginCredentialResult;
import com.tyb.myblog.v2.identity.domain.auth.LoginCredentialVerifier;
import com.tyb.myblog.v2.identity.domain.auth.LoginRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * 后台登录用例编排。
 */
@Service
@RequiredArgsConstructor
public class AuthApplicationService {

    private final LoginCredentialVerifier credentialVerifier;
    private final LoginRateLimiter rateLimiter;
    private final LoginSuccessTransactionService successTransactionService;
    private final Clock clock;

    /**
     * 执行后台登录，不在 BCrypt 和失败分支外层开启数据库事务。
     *
     * @param command 登录命令
     * @return 登录成功后的双 token
     */
    public LoginTokenResult login(LoginCommand command) {
        String username = command.username().trim().toLowerCase(Locale.ROOT);
        if (rateLimiter.isBlocked(command.clientIp(), username)) {
            throw new ApiException(ApiErrorCode.RATE_LIMITED);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LoginCredentialResult credentialResult = credentialVerifier.verify(
                username,
                command.password(),
                now);

        if (credentialResult instanceof LoginCredentialResult.BadCredentials) {
            rateLimiter.recordFailure(command.clientIp(), username);
            throw new ApiException(ApiErrorCode.BAD_CREDENTIALS);
        }
        if (credentialResult instanceof LoginCredentialResult.Locked) {
            throw new ApiException(ApiErrorCode.BAD_CREDENTIALS);
        }

        LoginCredentialResult.Authenticated authenticated =
                (LoginCredentialResult.Authenticated) credentialResult;
        rateLimiter.reset(command.clientIp(), username);
        return successTransactionService.complete(
                authenticated.account(),
                command.clientIp(),
                now);
    }
}
