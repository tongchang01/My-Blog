package com.tyb.myblog.v2.identity.infrastructure.security;

import com.tyb.myblog.v2.identity.domain.auth.PasswordHashService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 使用 Spring Security {@link PasswordEncoder} 处理后台账号密码摘要。
 */
@Component
@RequiredArgsConstructor
public class SpringPasswordHashService implements PasswordHashService {

    private final PasswordEncoder passwordEncoder;

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return passwordEncoder.matches(rawPassword, passwordHash);
    }

    @Override
    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
