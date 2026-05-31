package com.tyb.myblog.v2.identity.domain;

public record LoginCommand(String username, String password, String clientIp) {
    public LoginCommand(String username, String password) {
        this(username, password, null);
    }
}
