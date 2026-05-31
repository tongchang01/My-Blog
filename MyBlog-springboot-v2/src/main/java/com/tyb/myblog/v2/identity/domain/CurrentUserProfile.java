package com.tyb.myblog.v2.identity.domain;

public record CurrentUserProfile(
        String authId,
        String userInfoId,
        String username,
        String nickname,
        String avatar,
        String email
) {
}
