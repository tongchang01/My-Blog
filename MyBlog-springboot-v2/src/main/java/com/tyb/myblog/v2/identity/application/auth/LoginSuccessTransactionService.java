package com.tyb.myblog.v2.identity.application.auth;

import com.tyb.myblog.v2.common.auth.token.AccessTokenIssuer;
import com.tyb.myblog.v2.common.config.SecurityJwtProperties;
import com.tyb.myblog.v2.identity.application.token.RefreshTokenService;
import com.tyb.myblog.v2.identity.domain.account.UserAccount;
import com.tyb.myblog.v2.identity.domain.auth.LoginStateRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台登录成功后的短事务。
 */
@Service
@RequiredArgsConstructor
public class LoginSuccessTransactionService {

    private final LoginStateRecorder loginStateRecorder;
    private final RefreshTokenService refreshTokenService;
    private final AccessTokenIssuer accessTokenIssuer;
    private final SecurityJwtProperties jwtProperties;

    /**
     * 原子完成成功审计和 refresh token 持久化，再签发 access token。
     *
     * @param account 已通过凭据校验的后台账号
     * @param clientIp 可信客户端 IP，允许为空
     * @param loggedInAt 本次登录业务时间
     * @return 登录成功后的双 token
     */
    @Transactional
    public LoginTokenResult complete(
            UserAccount account,
            String clientIp,
            LocalDateTime loggedInAt
    ) {
        loginStateRecorder.recordSuccessfulLogin(
                account.id(),
                loggedInAt,
                clientIp);
        var refreshToken = refreshTokenService.issue(account.id());
        var accessToken = accessTokenIssuer.issueAccessToken(
                String.valueOf(account.id()),
                account.username(),
                List.of(account.type().name()),
                account.tokenVersion());

        return new LoginTokenResult(
                accessToken.accessToken(),
                refreshToken.token(),
                jwtProperties.accessTokenTtl().toSeconds(),
                jwtProperties.refreshTokenTtl().toSeconds());
    }
}
