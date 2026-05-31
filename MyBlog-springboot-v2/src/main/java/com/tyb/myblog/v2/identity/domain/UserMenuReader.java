package com.tyb.myblog.v2.identity.domain;

import java.util.List;

public interface UserMenuReader {

    List<UserMenu> findByAuthId(String authId);
}
