package com.tyb.myblog.v2.identity.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 登录账号持久化实体，对应 {@code t_user_auth}。
 */
@Getter
@Setter
@TableName("t_user_auth")
public class UserAccountEntity {

    /**
     * 账号主键。
     */
    @TableId(type = IdType.INPUT)
    private Long id;

    /**
     * 登录用户名。
     */
    private String username;

    /**
     * BCrypt 密码哈希。
     */
    private String passwordHash;

    /**
     * 账号类型数据库值。
     */
    private Integer type;

    /**
     * JWT 撤销版本号。
     */
    private Integer tokenVersion;

    /**
     * 连续登录失败次数。
     */
    private Integer loginFailCount;

    /**
     * 账号锁定截止时间。
     */
    private LocalDateTime lockedUntil;
}
