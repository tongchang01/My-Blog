package com.aurora.myblog.v2.modules.identity.domain;

import java.util.List;
import java.util.Optional;

public interface UserCredentialReader {
    Optional<UserCredential> findByUsername(String username);

    record UserCredential(String id, String username, String passwordHash, List<AuthRole> roles) {
    }
}
