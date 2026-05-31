package com.tyb.myblog.v2.identity.domain;

import java.util.List;
import java.util.Optional;

public interface UserCredentialReader {
    Optional<UserCredential> findByUsername(String username);

    record UserCredential(String id, String username, String passwordHash, List<AuthRole> roles) {
    }
}
