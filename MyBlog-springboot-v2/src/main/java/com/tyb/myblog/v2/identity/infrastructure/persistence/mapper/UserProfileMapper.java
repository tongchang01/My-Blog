package com.tyb.myblog.v2.identity.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyb.myblog.v2.identity.infrastructure.persistence.entity.UserProfileEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 用户资料持久化 Mapper。
 */
@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfileEntity> {

    /**
     * 按用户 ID 查询未删除资料及完整审计字段。
     *
     * @param userId 用户 ID
     * @return 未删除的资料实体，不存在时返回 {@code null}
     */
    UserProfileEntity selectActiveByUserId(@Param("userId") long userId);

    UserProfileEntity selectPrimaryPublicAuthor(@Param("now") LocalDateTime now);

    /**
     * 按用户 ID 加锁查询未删除资料。
     *
     * @param userId 用户 ID
     * @return 未删除的资料实体，不存在时返回 {@code null}
     */
    UserProfileEntity selectActiveByUserIdForUpdate(@Param("userId") long userId);

    /**
     * 更新未删除资料的全部可编辑字段和审计字段。
     *
     * @param profile   资料实体
     * @param updatedAt 更新时间
     * @param updatedBy 更新用户 ID
     * @return 影响行数
     */
    int updateActiveProfile(
            @Param("profile") UserProfileEntity profile,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("updatedBy") Long updatedBy);
}
