package com.tyb.myblog.v2.identity.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * refresh token 持久化实体，对应 {@code t_refresh_token}。
 *
 * <p>数据库仅保存 token 摘要，用于后台用户登录续期、单枚撤销和整体撤销。</p>
 */
@Getter
@Setter
@TableName("t_refresh_token")
public class RefreshTokenEntity {

    /**
     * refresh token 记录主键，由 MyBatis-Plus 在写入时生成。
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * token 所属后台用户 ID，不可为空。
     */
    private Long userId;

    /**
     * refresh token 明文的 SHA-256 摘要，不可为空且唯一。
     */
    private String tokenHash;

    /**
     * token 过期时间，不可为空，按应用统一时区保存。
     */
    private LocalDateTime expiresAt;

    /**
     * 撤销标记，不可为空；0 表示有效，1 表示已撤销。
     */
    private Integer revoked;

    /**
     * 记录创建时间，不可为空，由审计填充器写入。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 创建操作用户 ID；系统操作时可为空。
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    /**
     * 最近更新时间，不可为空，由审计填充器写入。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 最近操作用户 ID；系统操作时可为空。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;
}
