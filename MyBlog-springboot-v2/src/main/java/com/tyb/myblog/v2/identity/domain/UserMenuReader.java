package com.tyb.myblog.v2.identity.domain;

import java.util.List;

/**
 * 用户菜单读取端口。
 */
public interface UserMenuReader {

    /**
     * 根据认证账号 ID 查询用户可见的后台菜单树。
     */
    List<UserMenu> findByAuthId(String authId);
}
