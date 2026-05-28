package com.aurora.myblog.v2.modules.identity.domain;

import java.util.Optional;

public interface CurrentUserProfileReader {

    Optional<CurrentUserProfile> findByAuthId(String authId);
}
