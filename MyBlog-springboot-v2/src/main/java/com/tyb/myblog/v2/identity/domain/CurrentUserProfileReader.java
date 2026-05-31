package com.tyb.myblog.v2.identity.domain;

import java.util.Optional;

/**
 * 当前用户资料读取端口。
 */
public interface CurrentUserProfileReader {

    /**
     * 根据认证账号 ID 查询启用状态的用户资料。
     */
    Optional<CurrentUserProfile> findByAuthId(String authId);
}
