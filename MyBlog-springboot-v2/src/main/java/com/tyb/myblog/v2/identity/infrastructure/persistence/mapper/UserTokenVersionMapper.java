package com.tyb.myblog.v2.identity.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 后台账号 token 版本持久化 Mapper。
 */
@Mapper
public interface UserTokenVersionMapper {

    Integer selectActiveTokenVersion(@Param("userId") long userId);

    int incrementActiveTokenVersion(
            @Param("userId") long userId,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("updatedBy") Long updatedBy
    );
}
