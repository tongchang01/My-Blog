package com.tyb.myblog.v2.identity.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyb.myblog.v2.identity.infrastructure.persistence.entity.UserAccountEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

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

    /**
     * 原子累计一次密码错误，并在达到阈值时锁定账号。
     *
     * @param userId 账号 ID
     * @param failedAt 密码校验失败时间
     * @param maxAttempts 单个失败周期的锁定阈值
     * @param lockedUntil 达到阈值时写入的锁定截止时间
     * @return 更新行数
     */
    int recordPasswordFailure(
            @Param("userId") long userId,
            @Param("failedAt") LocalDateTime failedAt,
            @Param("maxAttempts") int maxAttempts,
            @Param("lockedUntil") LocalDateTime lockedUntil);
}
