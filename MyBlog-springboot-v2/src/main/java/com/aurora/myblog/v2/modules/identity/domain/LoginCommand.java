package com.aurora.myblog.v2.modules.identity.domain;

public record LoginCommand(String username, String password, String clientIp) {
    public LoginCommand(String username, String password) {
        this(username, password, null);
    }
}
