package com.aurora.myblog.v2.modules.identity.api;

import com.aurora.myblog.v2.modules.identity.domain.AuthRole;

import java.util.Set;

public record MeResponse(String id, String username, Set<AuthRole> roles) {
}
