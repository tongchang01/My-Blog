package com.tyb.myblog.v2.identity.domain;

/**
 * 系统角色。
 *
 * <p>当前只保留前台用户和后台管理员两类角色。旧库角色名称会在 infrastructure 中转换为该枚举。</p>
 */
public enum AuthRole {
    /**
     * 普通用户。
     */
    USER,
    /**
     * 后台管理员。
     */
    ADMIN
}
