package com.tyb.myblog.v2.identity.domain;

import java.util.Optional;

public interface CurrentUserProfileReader {

    Optional<CurrentUserProfile> findByAuthId(String authId);
}
