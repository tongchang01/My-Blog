package com.tyb.myblog.v2.identity.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyb.myblog.v2.identity.infrastructure.persistence.entity.UserProfileEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}
