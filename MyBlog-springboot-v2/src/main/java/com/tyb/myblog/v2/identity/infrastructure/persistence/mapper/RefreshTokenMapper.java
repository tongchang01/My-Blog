package com.tyb.myblog.v2.identity.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyb.myblog.v2.identity.infrastructure.persistence.entity.RefreshTokenEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface RefreshTokenMapper extends BaseMapper<RefreshTokenEntity> {

    @Select("""
            select id, user_id, token_hash, expires_at, revoked,
                   created_at, created_by, updated_at, updated_by
            from t_refresh_token
            where token_hash = #{tokenHash}
              and revoked = 0
              and expires_at > #{now}
            for update
            """)
    RefreshTokenEntity selectActiveForUpdate(
            @Param("tokenHash") String tokenHash,
            @Param("now") LocalDateTime now);
}
