package com.tyb.myblog.v2.identity.web;

import com.tyb.myblog.v2.identity.domain.UserMenu;

import java.util.List;

public record UserMenuResponse(
        String name,
        String path,
        String component,
        String icon,
        boolean hidden,
        List<UserMenuResponse> children
) {
    public static UserMenuResponse from(UserMenu menu) {
        return new UserMenuResponse(
                menu.name(),
                menu.path(),
                menu.component(),
                menu.icon(),
                menu.hidden(),
                menu.children().stream().map(UserMenuResponse::from).toList());
    }
}
