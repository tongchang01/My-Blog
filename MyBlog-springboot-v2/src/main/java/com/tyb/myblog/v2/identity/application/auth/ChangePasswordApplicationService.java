package com.tyb.myblog.v2.identity.application.auth;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.identity.domain.account.AccountType;
import com.tyb.myblog.v2.identity.domain.account.ChangeablePasswordAccount;
import com.tyb.myblog.v2.identity.domain.account.PasswordAccountRepository;
import com.tyb.myblog.v2.identity.domain.auth.PasswordHashService;
import com.tyb.myblog.v2.identity.domain.token.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 修改当前 ADMIN 密码并使其全部认证会话失效。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChangePasswordApplicationService {

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 128;

    private final PasswordAccountRepository passwordAccountRepository;
    private final PasswordHashService passwordHashService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    /**
     * 在单个事务中校验旧密码、写入新摘要并撤销全部 token。
     *
     * @param principal 当前认证主体
     * @param command 修改密码命令
     */
    @Transactional
    public void change(
            AuthenticatedPrincipal principal,
            ChangePasswordCommand command) {
        requireAdmin(principal);
        long userId = parsePositiveUserId(principal.id());
        validateCommand(command);

        ChangeablePasswordAccount account =
                passwordAccountRepository.findActiveByIdForUpdate(userId)
                        .orElseThrow(() -> missingAccount(userId));
        // 数据库账号类型是最终权限事实，避免使用尚未过期的旧角色快照完成改密。
        if (account.type() != AccountType.ADMIN) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        if (!passwordHashService.matches(
                command.currentPassword(),
                account.passwordHash())) {
            throw new ApiException(ApiErrorCode.BAD_CREDENTIALS);
        }
        if (passwordHashService.matches(
                command.newPassword(),
                account.passwordHash())) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "新密码不能与当前密码相同");
        }

        String newPasswordHash =
                passwordHashService.encode(command.newPassword());
        LocalDateTime updatedAt = LocalDateTime.now(clock);
        if (!passwordAccountRepository.updatePasswordAndIncrementTokenVersion(
                userId,
                newPasswordHash,
                updatedAt,
                userId)) {
            log.error("修改密码更新账号行数异常，userId={}", userId);
            throw new ApiException(ApiErrorCode.INTERNAL_ERROR);
        }
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    private void requireAdmin(AuthenticatedPrincipal principal) {
        if (principal == null || !principal.roles().contains("ADMIN")) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
    }

    private long parsePositiveUserId(String principalId) {
        try {
            long userId = Long.parseLong(principalId);
            if (userId <= 0) {
                throw new NumberFormatException();
            }
            return userId;
        } catch (NumberFormatException exception) {
            throw new ApiException(ApiErrorCode.INVALID_TOKEN);
        }
    }

    private void validateCommand(ChangePasswordCommand command) {
        if (command == null
                || command.currentPassword() == null
                || command.currentPassword().isBlank()
                || command.currentPassword().length() > MAX_PASSWORD_LENGTH
                || command.newPassword() == null
                || command.newPassword().isBlank()
                || command.newPassword().length() < MIN_PASSWORD_LENGTH
                || command.newPassword().length() > MAX_PASSWORD_LENGTH) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
        }
    }

    private ApiException missingAccount(long userId) {
        log.error("修改密码时当前账号不存在，userId={}", userId);
        return new ApiException(ApiErrorCode.INTERNAL_ERROR);
    }
}
