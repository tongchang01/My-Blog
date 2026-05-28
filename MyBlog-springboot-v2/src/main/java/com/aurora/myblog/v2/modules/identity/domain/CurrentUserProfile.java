package com.aurora.myblog.v2.modules.identity.domain;

public record CurrentUserProfile(
        String authId,
        String userInfoId,
        String username,
        String nickname,
        String avatar,
        String email
) {
}
