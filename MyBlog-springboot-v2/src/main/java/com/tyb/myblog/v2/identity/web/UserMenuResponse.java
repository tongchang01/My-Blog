package com.tyb.myblog.v2.identity.web;

import com.tyb.myblog.v2.identity.domain.UserMenu;

import java.util.List;

/**
 * 后台菜单响应节点。
 *
 * @param name      菜单名称
 * @param path      路由路径
 * @param component 前端组件路径
 * @param icon      菜单图标
 * @param hidden    是否隐藏
 * @param children  子菜单
 */
public record UserMenuResponse(
        String name,
        String path,
        String component,
        String icon,
        boolean hidden,
        List<UserMenuResponse> children
) {
    /**
     * 将领域菜单节点转换为接口响应。
     */
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
