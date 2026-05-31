package com.tyb.myblog.v2.identity.web;

import com.tyb.myblog.v2.identity.domain.AuthRole;

import java.util.Set;

/**
 * 当前登录用户资料响应。
 *
 * @param id         认证账号 ID
 * @param userInfoId 用户资料 ID
 * @param username   登录用户名
 * @param nickname   展示昵称
 * @param avatar     头像地址
 * @param email      邮箱地址
 * @param roles      当前用户角色
 */
public record MeResponse(
        String id,
        String userInfoId,
        String username,
        String nickname,
        String avatar,
        String email,
        Set<AuthRole> roles
) {
}
