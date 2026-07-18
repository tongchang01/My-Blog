package com.tyb.myblog.v2.content.domain.article;

/**
 * 文章访问密码哈希端口。
 */
public interface ArticlePasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String passwordHash);
}
