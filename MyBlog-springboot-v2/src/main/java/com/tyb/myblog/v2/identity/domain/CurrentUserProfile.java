package com.tyb.myblog.v2.identity.domain;

/**
 * 当前登录用户资料。
 *
 * <p>聚合旧库认证账号表和用户资料表的展示字段，用于 `/api/auth/me`。</p>
 *
 * @param authId     认证账号 ID，对应 {@code t_user_auth.id}
 * @param userInfoId 用户资料 ID，对应 {@code t_user_info.id}
 * @param username   登录用户名
 * @param nickname   展示昵称
 * @param avatar     头像地址
 * @param email      邮箱地址
 */
public record CurrentUserProfile(
        String authId,
        String userInfoId,
        String username,
        String nickname,
        String avatar,
        String email
) {
}
