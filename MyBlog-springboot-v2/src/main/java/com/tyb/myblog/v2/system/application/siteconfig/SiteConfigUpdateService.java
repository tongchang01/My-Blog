package com.tyb.myblog.v2.system.application.siteconfig;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.system.domain.siteconfig.SiteConfig;
import com.tyb.myblog.v2.system.domain.siteconfig.SiteConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 站点配置全量更新服务，负责权限、事务和并发一致性边界。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SiteConfigUpdateService {

    private final SiteConfigRepository repository;
    private final Clock clock;

    /**
     * 由 ADMIN 完整覆盖固定站点配置。
     */
    @Transactional
    public AdminSiteConfigResult update(
            AuthenticatedPrincipal principal,
            UpdateSiteConfigCommand command) {
        requireAdmin(principal);
        long userId = parsePositiveUserId(principal.id());
        if (command == null) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "站点配置请求不能为空");
        }

        SiteConfig current = repository.findActiveForUpdate()
                .orElseThrow(this::missingConfig);
        SiteConfig updated;
        try {
            updated = command.toDomain(current);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    exception.getMessage());
        }

        LocalDateTime updatedAt = LocalDateTime.now(clock);
        if (!repository.update(updated, updatedAt, userId)) {
            log.error(
                    "站点配置更新行数异常，siteConfigId={}",
                    SiteConfig.FIXED_ID);
            throw new ApiException(ApiErrorCode.INTERNAL_ERROR);
        }
        return AdminSiteConfigResult.from(
                repository.findActive().orElseThrow(this::missingConfig));
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

    private ApiException missingConfig() {
        log.error("站点配置固定行不存在，siteConfigId={}", SiteConfig.FIXED_ID);
        return new ApiException(ApiErrorCode.INTERNAL_ERROR);
    }
}
