package com.tyb.myblog.v2.identity.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 用户 token 版本持久化 Mapper。
 */
@Mapper
public interface UserTokenVersionMapper {

    /**
     * 查询未删除用户的当前 token 版本。
     */
    Integer selectActiveTokenVersion(@Param("userId") long userId);

    /**
     * 查询未删除且不在锁定期的后台用户当前 token 版本。
     */
    Integer selectRefreshableTokenVersion(
            @Param("userId") long userId,
            @Param("now") LocalDateTime now);

    /**
     * 递增未删除用户的 token 版本，并写入应用层提供的审计时间和实际操作者。
     */
    int incrementActiveTokenVersion(
            @Param("userId") long userId,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("updatedBy") Long updatedBy);
}
