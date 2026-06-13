package com.tyb.myblog.v2.identity.web;

import com.tyb.myblog.v2.identity.domain.profile.UserProfile;

/**
 * 当前用户公开资料响应，不暴露内部用户 ID。
 *
 * @param nickname 昵称
 * @param avatarUrl 头像地址
 * @param bioZh 中文简介
 * @param bioJa 日文简介
 * @param bioEn 英文简介
 * @param location 所在地
 * @param website 个人主页
 * @param emailPublic 公开邮箱
 * @param githubUrl GitHub 地址
 * @param twitterUrl Twitter 地址
 * @param linkedinUrl LinkedIn 地址
 * @param zhihuUrl 知乎地址
 * @param qiitaUrl Qiita 地址
 * @param juejinUrl 掘金地址
 */
public record UserProfileVO(
        String nickname,
        String avatarUrl,
        String bioZh,
        String bioJa,
        String bioEn,
        String location,
        String website,
        String emailPublic,
        String githubUrl,
        String twitterUrl,
        String linkedinUrl,
        String zhihuUrl,
        String qiitaUrl,
        String juejinUrl
) {

    /**
     * 将领域资料转换为 HTTP 响应。
     *
     * @param profile 用户资料领域对象
     * @return 用户资料响应
     */
    public static UserProfileVO from(UserProfile profile) {
        return new UserProfileVO(
                profile.nickname(),
                profile.avatarUrl(),
                profile.bioZh(),
                profile.bioJa(),
                profile.bioEn(),
                profile.location(),
                profile.website(),
                profile.emailPublic(),
                profile.githubUrl(),
                profile.twitterUrl(),
                profile.linkedinUrl(),
                profile.zhihuUrl(),
                profile.qiitaUrl(),
                profile.juejinUrl());
    }
}
