package com.tyb.myblog.v2.system.application.friendlink;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.system.domain.friendlink.FriendLink;
import com.tyb.myblog.v2.system.domain.friendlink.FriendLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 后台友链完整编辑服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FriendLinkUpdateService {

    private final FriendLinkRepository repository;
    private final FriendLinkAuthorization authorization;
    private final Clock clock;

    /**
     * 锁定 active 行后完整替换业务字段，避免并发编辑和删除交叉覆盖。
     */
    @Transactional
    public FriendLinkResult update(
            AuthenticatedPrincipal principal,
            long id,
            UpdateFriendLinkCommand command) {
        long actorId = authorization.requireAdmin(principal);
        validateId(id);
        if (command == null) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "友链请求不能为空");
        }
        FriendLink current = repository.findActiveByIdForUpdate(id)
                .orElseThrow(this::notFound);
        FriendLink updated;
        try {
            updated = current.replace(
                    command.name(),
                    command.url(),
                    command.avatarUrl(),
                    command.description(),
                    command.sortOrder(),
                    command.status());
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    exception.getMessage());
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (!repository.update(updated, now, actorId)) {
            log.error("友链更新行数异常，friendLinkId={}", id);
            throw new ApiException(ApiErrorCode.INTERNAL_ERROR);
        }
        return repository.findActiveById(id)
                .map(FriendLinkResult::from)
                .orElseThrow(this::missingAfterUpdate);
    }

    private void validateId(long id) {
        if (id <= 0) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "友链 ID 必须为正数");
        }
    }

    private ApiException notFound() {
        return new ApiException(
                ApiErrorCode.NOT_FOUND,
                "友链不存在");
    }

    private ApiException missingAfterUpdate() {
        log.error("友链更新后无法重新读取");
        return new ApiException(ApiErrorCode.INTERNAL_ERROR);
    }
}
