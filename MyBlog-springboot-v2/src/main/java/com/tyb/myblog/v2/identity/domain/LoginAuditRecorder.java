package com.tyb.myblog.v2.identity.domain;

public interface LoginAuditRecorder {
    void recordSuccessfulLogin(String authId, String clientIp);
}
