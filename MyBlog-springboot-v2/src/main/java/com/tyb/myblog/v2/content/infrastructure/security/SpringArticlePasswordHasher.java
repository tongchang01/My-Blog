package com.tyb.myblog.v2.content.infrastructure.security;

import com.tyb.myblog.v2.content.domain.article.ArticlePasswordHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 基于 Spring Security PasswordEncoder 的文章访问密码哈希实现。
 */
@Component
@RequiredArgsConstructor
public class SpringArticlePasswordHasher implements ArticlePasswordHasher {

    private static final int MAX_RAW_PASSWORD_LENGTH = 200;

    private final PasswordEncoder passwordEncoder;

    @Override
    public String hash(String rawPassword) {
        if (rawPassword == null
                || rawPassword.isBlank()
                || rawPassword.length() > MAX_RAW_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("文章密码格式非法");
        }
        return passwordEncoder.encode(rawPassword);
    }
}
