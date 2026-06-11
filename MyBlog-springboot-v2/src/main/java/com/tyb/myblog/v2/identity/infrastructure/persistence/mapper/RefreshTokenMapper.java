package com.tyb.myblog.v2.identity.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyb.myblog.v2.identity.infrastructure.persistence.entity.RefreshTokenEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * refresh token 持久化 Mapper。
 */
@Mapper
public interface RefreshTokenMapper extends BaseMapper<RefreshTokenEntity> {

    /**
     * 锁定仍有效的 refresh token，确保轮换操作只能消费旧 token 一次。
     */
    RefreshTokenEntity selectActiveForUpdate(
            @Param("tokenHash") String tokenHash,
            @Param("now") LocalDateTime now);
}
