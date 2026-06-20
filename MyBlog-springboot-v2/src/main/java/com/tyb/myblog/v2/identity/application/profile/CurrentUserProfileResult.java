package com.tyb.myblog.v2.identity.application.profile;

import com.tyb.myblog.v2.identity.domain.account.AccountType;

/**
 * 当前用户账号与个人资料的组合查询结果。
 *
 * @param id 账号 ID
 * @param username 登录用户名
 * @param type 账号类型
 * @param profile 个人资料
 */
public record CurrentUserProfileResult(
        long id,
        String username,
        AccountType type,
        UserProfileResult profile
) {
}
