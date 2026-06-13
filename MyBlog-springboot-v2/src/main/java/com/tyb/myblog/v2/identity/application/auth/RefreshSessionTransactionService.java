package com.tyb.myblog.v2.identity.application.auth;

import com.tyb.myblog.v2.common.auth.token.AccessTokenIssuer;
import com.tyb.myblog.v2.common.config.SecurityJwtProperties;
import com.tyb.myblog.v2.identity.application.token.IssuedRefreshToken;
import com.tyb.myblog.v2.identity.application.token.RefreshTokenService;
import com.tyb.myblog.v2.identity.domain.account.RefreshableAccount;
import com.tyb.myblog.v2.identity.domain.account.RefreshableAccountRepository;
import com.tyb.myblog.v2.identity.domain.token.RefreshTokenRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 在同一事务内完成 refresh token 锁定、撤销和双 token 重签。
 */
@Service
@RequiredArgsConstructor
public class RefreshSessionTransactionService {

    private final RefreshTokenService refreshTokenService;
    private final RefreshableAccountRepository accountRepository;
    private final AccessTokenIssuer accessTokenIssuer;
    private final SecurityJwtProperties jwtProperties;
    private final Clock clock;

    /**
     * 原子轮换 refresh token；JWT 签发失败时数据库修改整体回滚。
     */
    @Transactional
    public Optional<LoginTokenResult> refresh(String rawRefreshToken) {
        LocalDateTime now = LocalDateTime.now(clock);
        Optional<RefreshTokenRecord> tokenOptional =
                refreshTokenService.findActiveForUpdate(
                        rawRefreshToken,
                        now);
        if (tokenOptional.isEmpty()) {
            return Optional.empty();
        }

        RefreshTokenRecord token = tokenOptional.get();
        Optional<RefreshableAccount> accountOptional =
                accountRepository.findRefreshableById(
                        token.userId(),
                        now);
        if (accountOptional.isEmpty()) {
            refreshTokenService.revoke(token.id());
            return Optional.empty();
        }

        if (!refreshTokenService.revoke(token.id())) {
            return Optional.empty();
        }

        RefreshableAccount account = accountOptional.get();
        IssuedRefreshToken refreshToken =
                refreshTokenService.issue(account.id());
        var accessToken = accessTokenIssuer.issueAccessToken(
                String.valueOf(account.id()),
                account.username(),
                List.of(account.type().name()),
                account.tokenVersion());

        return Optional.of(new LoginTokenResult(
                accessToken.accessToken(),
                refreshToken.token(),
                jwtProperties.accessTokenTtl().toSeconds(),
                jwtProperties.refreshTokenTtl().toSeconds()));
    }
}
