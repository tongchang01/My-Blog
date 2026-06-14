package com.tyb.myblog.v2.system.application.friendlink;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.system.domain.friendlink.FriendLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 友链展示状态修改服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FriendLinkStatusService {

    private final FriendLinkRepository repository;
    private final FriendLinkAuthorization authorization;
    private final Clock clock;

    @Transactional
    public FriendLinkResult update(
            AuthenticatedPrincipal principal,
            long id,
            UpdateFriendLinkStatusCommand command) {
        long actorId = authorization.requireAdmin(principal);
        validate(id, command);
        repository.findActiveByIdForUpdate(id)
                .orElseThrow(this::notFound);
        LocalDateTime now = LocalDateTime.now(clock);
        if (!repository.updateStatus(
                id, command.status(), now, actorId)) {
            log.error("友链状态更新行数异常，friendLinkId={}", id);
            throw new ApiException(ApiErrorCode.INTERNAL_ERROR);
        }
        return repository.findActiveById(id)
                .map(FriendLinkResult::from)
                .orElseThrow(this::missingAfterUpdate);
    }

    private void validate(
            long id,
            UpdateFriendLinkStatusCommand command) {
        if (id <= 0) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "友链 ID 必须为正数");
        }
        if (command == null || command.status() == null) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "友链状态不能为空");
        }
    }

    private ApiException notFound() {
        return new ApiException(
                ApiErrorCode.NOT_FOUND,
                "友链不存在");
    }

    private ApiException missingAfterUpdate() {
        log.error("友链状态更新后无法重新读取");
        return new ApiException(ApiErrorCode.INTERNAL_ERROR);
    }
}
