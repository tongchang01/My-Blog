package com.tyb.myblog.v2.identity.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyb.myblog.v2.identity.infrastructure.persistence.entity.UserAccountEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 登录账号持久化 Mapper。
 */
@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccountEntity> {

    /**
     * 按用户名查询未删除账号。
     *
     * @param username 登录用户名
     * @return 匹配的账号实体，不存在时返回 {@code null}
     */
    UserAccountEntity selectActiveByUsername(@Param("username") String username);
}
