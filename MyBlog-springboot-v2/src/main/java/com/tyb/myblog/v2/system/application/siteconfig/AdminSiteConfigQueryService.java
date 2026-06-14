package com.tyb.myblog.v2.system.application.siteconfig;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.system.domain.siteconfig.SiteConfig;
import com.tyb.myblog.v2.system.domain.siteconfig.SiteConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 查询后台完整站点配置。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSiteConfigQueryService {

    private final SiteConfigRepository repository;

    /**
     * 查询完整配置，允许 ADMIN 和 DEMO 只读访问。
     */
    public AdminSiteConfigResult query(AuthenticatedPrincipal principal) {
        requireReadableRole(principal);
        SiteConfig config = repository.findActive()
                .orElseThrow(this::missingConfig);
        return AdminSiteConfigResult.from(config);
    }

    private void requireReadableRole(AuthenticatedPrincipal principal) {
        if (principal == null) {
            throw new ApiException(ApiErrorCode.INVALID_TOKEN);
        }
        boolean readable = principal.roles().stream()
                .anyMatch(role -> "ADMIN".equals(role) || "DEMO".equals(role));
        if (!readable) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
    }

    private ApiException missingConfig() {
        log.error("站点配置固定行不存在，siteConfigId={}", SiteConfig.FIXED_ID);
        return new ApiException(ApiErrorCode.INTERNAL_ERROR);
    }
}
