package com.tyb.myblog.v2.identity.domain;

import java.util.List;

/**
 * 后台菜单节点。
 *
 * <p>对应旧库菜单表的树形结构，用于后台左侧菜单和路由生成。</p>
 *
 * @param name      菜单名称
 * @param path      路由路径
 * @param component 前端组件路径
 * @param icon      菜单图标
 * @param hidden    是否在菜单中隐藏
 * @param children  子菜单列表
 */
public record UserMenu(
        String name,
        String path,
        String component,
        String icon,
        boolean hidden,
        List<UserMenu> children
) {
    /**
     * 复制子菜单列表，避免菜单树在返回后被外部修改。
     */
    public UserMenu {
        children = children == null ? List.of() : List.copyOf(children);
    }
}
