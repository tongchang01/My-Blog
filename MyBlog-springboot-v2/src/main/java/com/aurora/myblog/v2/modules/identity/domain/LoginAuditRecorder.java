package com.aurora.myblog.v2.modules.identity.domain;

public interface LoginAuditRecorder {
    void recordSuccessfulLogin(String authId, String clientIp);
}
