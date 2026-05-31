package com.tyb.myblog.v2.identity.web;

import com.tyb.myblog.v2.identity.domain.AuthRole;

import java.util.Set;

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
