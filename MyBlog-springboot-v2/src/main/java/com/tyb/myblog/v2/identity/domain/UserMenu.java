package com.tyb.myblog.v2.identity.domain;

import java.util.List;

public record UserMenu(
        String name,
        String path,
        String component,
        String icon,
        boolean hidden,
        List<UserMenu> children
) {
    public UserMenu {
        children = children == null ? List.of() : List.copyOf(children);
    }
}
