package com.tyb.myblog.v2.identity.web;

import com.tyb.myblog.v2.identity.application.profile.CurrentUserProfileResult;
import com.tyb.myblog.v2.identity.domain.account.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 当前登录账号及其公开资料响应。
 *
 * @param id 账号 ID
 * @param username 登录用户名
 * @param type 账号类型
 * @param profile 公开资料
 */
public record CurrentUserVO(
        @Schema(format = "int64", example = "9007199254740993")
        String id,
        String username,
        AccountType type,
        UserProfileVO profile
) {

    /**
     * 将应用查询结果转换为 HTTP 响应。
     *
     * @param result 当前用户资料查询结果
     * @return 当前登录账号响应
     */
    public static CurrentUserVO from(CurrentUserProfileResult result) {
        return new CurrentUserVO(
                Long.toString(result.id()),
                result.username(),
                result.type(),
                UserProfileVO.from(result.profile()));
    }
}
