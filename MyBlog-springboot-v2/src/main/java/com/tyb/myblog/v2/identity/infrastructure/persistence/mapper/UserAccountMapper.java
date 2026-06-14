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
     * 按账号 ID 查询未删除账号的安全投影。
     *
     * @param userId 账号 ID
     * @return 当前账号实体，不存在时返回 {@code null}
     */
    UserAccountEntity selectActiveById(@Param("userId") long userId);

    /**
     * 按用户名查询未删除账号。
     *
     * @param username 登录用户名
     * @return 匹配的账号实体，不存在时返回 {@code null}
     */
    UserAccountEntity selectActiveByUsername(@Param("username") String username);

    /**
     * 锁定并读取未删除账号的改密字段。
     *
     * @param userId 账号 ID
     * @return 改密账号实体，不存在时返回 {@code null}
     */
    UserAccountEntity selectActivePasswordAccountForUpdate(
            @Param("userId") long userId);

    /**
     * 查询未删除、未锁定且允许登录后台的账号快照。
     *
     * @param userId 账号 ID
     * @param now 当前业务时间
     * @return 可刷新账号实体，不满足条件时返回 {@code null}
     */
    UserAccountEntity selectRefreshableById(
            @Param("userId") long userId,
            @Param("now") LocalDateTime now);

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

    /**
     * 写入登录成功审计信息并清理失败状态。
     *
     * @param userId 账号 ID
     * @param loggedInAt 登录成功时间
     * @param clientIp 客户端 IP
     * @return 更新行数
     */
    int recordSuccessfulLogin(
            @Param("userId") long userId,
            @Param("loggedInAt") LocalDateTime loggedInAt,
            @Param("clientIp") String clientIp);

    /**
     * 原子更新密码摘要并递增 token version。
     *
     * @param userId 账号 ID
     * @param passwordHash 新密码摘要
     * @param updatedAt 更新时间
     * @param updatedBy 操作人 ID
     * @return 更新行数
     */
    int updatePasswordAndIncrementTokenVersion(
            @Param("userId") long userId,
            @Param("passwordHash") String passwordHash,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("updatedBy") Long updatedBy);
}
