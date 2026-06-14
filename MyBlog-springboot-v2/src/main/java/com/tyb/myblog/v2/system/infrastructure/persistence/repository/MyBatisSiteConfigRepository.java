package com.tyb.myblog.v2.system.infrastructure.persistence.repository;

import com.tyb.myblog.v2.system.domain.siteconfig.SiteConfig;
import com.tyb.myblog.v2.system.domain.siteconfig.SiteConfigRepository;
import com.tyb.myblog.v2.system.infrastructure.persistence.entity.SiteConfigEntity;
import com.tyb.myblog.v2.system.infrastructure.persistence.mapper.SiteConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 基于 MyBatis 的站点配置仓储适配器。
 */
@Repository
@RequiredArgsConstructor
public class MyBatisSiteConfigRepository implements SiteConfigRepository {

    private final SiteConfigMapper mapper;

    @Override
    public Optional<SiteConfig> findActive() {
        return Optional.ofNullable(mapper.selectActive())
                .map(this::toDomain);
    }

    @Override
    public Optional<SiteConfig> findActiveForUpdate() {
        return Optional.ofNullable(mapper.selectActiveForUpdate())
                .map(this::toDomain);
    }

    @Override
    public boolean update(
            SiteConfig config,
            LocalDateTime updatedAt,
            Long updatedBy) {
        return mapper.updateActive(
                toEntity(config),
                updatedAt,
                updatedBy) == 1;
    }

    private SiteConfig toDomain(SiteConfigEntity entity) {
        return SiteConfig.create(
                entity.getId(),
                entity.getSiteTitleZh(),
                entity.getSiteTitleJa(),
                entity.getSiteTitleEn(),
                entity.getSiteSubtitleZh(),
                entity.getSiteSubtitleJa(),
                entity.getSiteSubtitleEn(),
                entity.getAboutMdZh(),
                entity.getAboutMdJa(),
                entity.getAboutMdEn(),
                entity.getLogoUrl(),
                entity.getFaviconUrl(),
                entity.getIcpNo(),
                entity.getSpotifyPlaylistId(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy());
    }

    private SiteConfigEntity toEntity(SiteConfig config) {
        SiteConfigEntity entity = new SiteConfigEntity();
        entity.setId(config.id());
        entity.setSiteTitleZh(config.siteTitleZh());
        entity.setSiteTitleJa(config.siteTitleJa());
        entity.setSiteTitleEn(config.siteTitleEn());
        entity.setSiteSubtitleZh(config.siteSubtitleZh());
        entity.setSiteSubtitleJa(config.siteSubtitleJa());
        entity.setSiteSubtitleEn(config.siteSubtitleEn());
        entity.setAboutMdZh(config.aboutMdZh());
        entity.setAboutMdJa(config.aboutMdJa());
        entity.setAboutMdEn(config.aboutMdEn());
        entity.setLogoUrl(config.logoUrl());
        entity.setFaviconUrl(config.faviconUrl());
        entity.setIcpNo(config.icpNo());
        entity.setSpotifyPlaylistId(config.spotifyPlaylistId());
        return entity;
    }
}
