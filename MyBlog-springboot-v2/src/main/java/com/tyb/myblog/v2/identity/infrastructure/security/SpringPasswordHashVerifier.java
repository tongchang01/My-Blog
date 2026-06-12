package com.tyb.myblog.v2.identity.infrastructure.security;

import com.tyb.myblog.v2.identity.domain.auth.PasswordHashVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 使用 Spring Security {@link PasswordEncoder} 校验后台账号密码摘要。
 */
@Component
@RequiredArgsConstructor
public class SpringPasswordHashVerifier implements PasswordHashVerifier {

    private final PasswordEncoder passwordEncoder;

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return passwordEncoder.matches(rawPassword, passwordHash);
    }
}
