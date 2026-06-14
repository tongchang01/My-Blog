package com.tyb.myblog.v2.system.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyb.myblog.v2.system.infrastructure.persistence.entity.SiteConfigEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 站点配置持久化 Mapper。
 */
@Mapper
public interface SiteConfigMapper extends BaseMapper<SiteConfigEntity> {

    /** 查询未删除的固定配置行。 */
    SiteConfigEntity selectActive();

    /** 加锁查询未删除的固定配置行。 */
    SiteConfigEntity selectActiveForUpdate();

    /** 完整更新固定配置行和修改审计字段。 */
    int updateActive(
            @Param("config") SiteConfigEntity config,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("updatedBy") Long updatedBy);
}
