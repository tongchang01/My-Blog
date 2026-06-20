package com.tyb.myblog.v2.identity.application.profile;

import com.tyb.myblog.v2.identity.domain.profile.UserProfile;

/**
 * 用户资料应用结果。
 */
public record UserProfileResult(
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
        String juejinUrl) {

    public static UserProfileResult from(UserProfile profile) {
        return new UserProfileResult(
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
