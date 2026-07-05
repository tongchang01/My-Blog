package com.tyb.myblog.v2.system.application.siteconfig;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.system.domain.siteconfig.SiteConfig;
import com.tyb.myblog.v2.system.domain.siteconfig.SiteConfigRepository;
import com.tyb.myblog.v2.system.domain.siteconfig.SiteLanguage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 查询前台当前语言站点配置。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublicSiteConfigQueryService {

    private final SiteConfigRepository repository;

    /**
     * 按语言查询公开配置，并对缺失翻译逐字段回退中文。
     */
    public PublicSiteConfigResult query(String languageCode) {
        SiteLanguage language;
        try {
            language = SiteLanguage.parse(languageCode);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    exception.getMessage());
        }

        SiteConfig config = repository.findActive()
                .orElseThrow(this::missingConfig);
        return new PublicSiteConfigResult(
                config.siteTitle(language),
                config.siteSubtitle(language),
                config.aboutMd(language),
                config.logoUrl(),
                config.faviconUrl(),
                config.icpNo(),
                config.spotifyPlaylistId(),
                config.startedDate());
    }

    private ApiException missingConfig() {
        log.error(
                "站点配置固定行不存在，siteConfigId={}",
                SiteConfig.FIXED_ID);
        return new ApiException(ApiErrorCode.INTERNAL_ERROR);
    }
}
