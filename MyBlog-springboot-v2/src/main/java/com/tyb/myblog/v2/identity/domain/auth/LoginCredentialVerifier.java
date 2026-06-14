package com.tyb.myblog.v2.identity.domain.auth;

import com.tyb.myblog.v2.identity.domain.account.UserAccount;
import com.tyb.myblog.v2.identity.domain.account.UserAccountRepository;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 后台登录凭据领域校验器。
 */
@RequiredArgsConstructor
public class LoginCredentialVerifier {

    private final UserAccountRepository repository;
    private final PasswordHashService passwordHashService;
    private final LoginStateRecorder loginStateRecorder;
    private final LoginLockPolicy loginLockPolicy;

    /**
     * 校验后台登录凭据。
     *
     * @param username 登录用户名
     * @param rawPassword 明文密码
     * @param now 当前业务时间
     * @return 不包含 HTTP 语义的领域校验结果
     */
    public LoginCredentialResult verify(String username, String rawPassword, LocalDateTime now) {
        Optional<UserAccount> candidate = repository.findActiveByUsername(username);
        if (candidate.isEmpty()) {
            return LoginCredentialResult.BadCredentials.INSTANCE;
        }

        UserAccount account = candidate.orElseThrow();
        if (!account.canLoginToAdmin()) {
            return LoginCredentialResult.BadCredentials.INSTANCE;
        }
        if (account.isLockedAt(now)) {
            return LoginCredentialResult.Locked.INSTANCE;
        }

        if (passwordHashService.matches(rawPassword, account.passwordHash())) {
            return new LoginCredentialResult.Authenticated(account);
        }

        // 仅对已确认可登录后台的账号累计密码失败，避免为不存在账号或 GUEST 写状态。
        loginStateRecorder.recordPasswordFailure(
                account.id(),
                now,
                loginLockPolicy.maxAttempts(),
                now.plus(loginLockPolicy.lockDuration()));
        return LoginCredentialResult.BadCredentials.INSTANCE;
    }
}
