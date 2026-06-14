package com.tyb.myblog.v2.system.domain.siteconfig;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 站点配置仓储端口，隔离领域模型与数据库实现。
 */
public interface SiteConfigRepository {

    /**
     * 查询未删除的固定站点配置。
     */
    Optional<SiteConfig> findActive();

    /**
     * 加锁查询未删除的固定站点配置。
     */
    Optional<SiteConfig> findActiveForUpdate();

    /**
     * 完整更新站点配置和修改审计字段。
     */
    boolean update(
            SiteConfig config,
            LocalDateTime updatedAt,
            Long updatedBy);
}
