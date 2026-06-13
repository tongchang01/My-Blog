package com.tyb.myblog.v2.identity.application.profile;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.identity.domain.profile.UserProfile;
import com.tyb.myblog.v2.identity.domain.profile.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 当前用户资料部分更新服务，负责权限、事务和数据一致性边界。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CurrentUserProfileUpdateService {

    private final UserProfileRepository repository;

    /**
     * 更新当前 ADMIN 用户的个人资料。
     *
     * @param principal 当前认证主体
     * @param command   部分更新命令
     * @return 更新后的完整用户资料
     */
    @Transactional
    public UserProfile update(
            AuthenticatedPrincipal principal,
            UpdateCurrentUserProfileCommand command) {
        requireAdmin(principal);
        long userId = parsePositiveUserId(principal.id());
        if (command == null || !command.hasAnyPresentField()) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "至少提交一个资料字段");
        }

        UserProfile current = repository.findActiveByUserIdForUpdate(userId)
                .orElseThrow(() -> profileMissing(userId));
        UserProfile updated;
        try {
            updated = current.apply(command.toDomainPatch());
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    exception.getMessage());
        }

        if (!repository.update(updated)) {
            log.error("当前用户资料更新行数异常，userId={}", userId);
            throw new ApiException(ApiErrorCode.INTERNAL_ERROR);
        }
        return updated;
    }

    private void requireAdmin(AuthenticatedPrincipal principal) {
        if (principal == null) {
            throw new ApiException(ApiErrorCode.INVALID_TOKEN);
        }
        if (!principal.roles().contains("ADMIN")) {
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

    private ApiException profileMissing(long userId) {
        log.error("当前用户资料不存在，userId={}", userId);
        return new ApiException(ApiErrorCode.INTERNAL_ERROR);
    }
}
