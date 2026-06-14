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
 * 友链显式软删除服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FriendLinkDeleteService {

    private final FriendLinkRepository repository;
    private final FriendLinkAuthorization authorization;
    private final Clock clock;

    /**
     * 锁定 active 行并同时写入删除与更新审计字段。
     */
    @Transactional
    public void delete(
            AuthenticatedPrincipal principal,
            long id) {
        long actorId = authorization.requireAdmin(principal);
        if (id <= 0) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "友链 ID 必须为正数");
        }
        repository.findActiveByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(
                        ApiErrorCode.NOT_FOUND,
                        "友链不存在"));
        LocalDateTime now = LocalDateTime.now(clock);
        if (!repository.softDelete(id, now, actorId)) {
            log.error("友链软删除行数异常，friendLinkId={}", id);
            throw new ApiException(ApiErrorCode.INTERNAL_ERROR);
        }
    }
}
