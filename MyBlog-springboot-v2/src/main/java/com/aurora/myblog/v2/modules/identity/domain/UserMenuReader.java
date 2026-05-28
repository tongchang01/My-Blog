package com.aurora.myblog.v2.modules.identity.domain;

import java.util.List;

public interface UserMenuReader {

    List<UserMenu> findByAuthId(String authId);
}
