package com.tyb.myblog.v2.content.application.article;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.domain.article.ArticleAccessTokenRecord;
import com.tyb.myblog.v2.content.domain.article.ArticleAccessTokenRepository;
import com.tyb.myblog.v2.content.domain.article.ArticlePasswordHasher;
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import com.tyb.myblog.v2.content.domain.article.PublicArticleAccessMetadata;
import com.tyb.myblog.v2.content.domain.article.PublicArticleQueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/** PASSWORD 文章的短期独立授权。 */
@Service
public class PublicArticleAccessService {

    private static final int TOKEN_BYTES = 32;
    private static final Duration TOKEN_TTL = Duration.ofHours(24);

    private final PublicArticleQueryRepository articleRepository;
    private final ArticleAccessTokenRepository tokenRepository;
    private final ArticlePasswordHasher passwordHasher;
    private final ArticleAccessRateLimitService rateLimitService;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public PublicArticleAccessService(
            PublicArticleQueryRepository articleRepository,
            ArticleAccessTokenRepository tokenRepository,
            ArticlePasswordHasher passwordHasher,
            ArticleAccessRateLimitService rateLimitService,
            Clock clock) {
        this.articleRepository = articleRepository;
        this.tokenRepository = tokenRepository;
        this.passwordHasher = passwordHasher;
        this.rateLimitService = rateLimitService;
        this.clock = clock;
    }

    @Transactional
    public ArticleAccessTokenResult unlock(
            long articleId,
            String password,
            String clientIp) {
        if (articleId <= 0) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "文章 ID 必须为正数");
        }
        rateLimitService.checkAndRecord(clientIp, articleId);
        LocalDateTime now = LocalDateTime.now(clock);
        PublicArticleAccessMetadata article = findPublic(articleId, now);
        if (article.status() != ArticleStatus.PASSWORD
                || !passwordHasher.matches(password, article.accessPassword())) {
            throw new ApiException(ApiErrorCode.FORBIDDEN, "文章密码错误或文章不可解锁");
        }
        String rawToken = generateToken();
        LocalDateTime expiresAt = now.plus(TOKEN_TTL);
        tokenRepository.save(new ArticleAccessTokenRecord(
                articleId,
                hash(rawToken),
                expiresAt));
        return new ArticleAccessTokenResult(rawToken, expiresAt);
    }

    public void requireAccess(
            PublicArticleAccessMetadata article,
            String rawToken,
            LocalDateTime now) {
        if (article.status() != ArticleStatus.PASSWORD) {
            return;
        }
        if (rawToken == null
                || rawToken.isBlank()
                || !tokenRepository.existsActive(article.id(), hash(rawToken), now)) {
            throw new ApiException(ApiErrorCode.FORBIDDEN, "密码文章需要有效访问授权");
        }
    }

    public void requirePasswordAccess(long articleId, String rawToken) {
        LocalDateTime now = LocalDateTime.now(clock);
        PublicArticleAccessMetadata article = findPublic(articleId, now);
        if (article.status() != ArticleStatus.PASSWORD) {
            throw new ApiException(ApiErrorCode.NOT_FOUND);
        }
        requireAccess(article, rawToken, now);
    }

    @Transactional
    public void revokeAll(long articleId) {
        tokenRepository.revokeAllByArticleId(articleId);
    }

    private PublicArticleAccessMetadata findPublic(long articleId, LocalDateTime now) {
        return articleRepository.findPublicAccessMetadata(articleId, now)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND, "文章不存在"));
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 JVM 不支持 SHA-256", exception);
        }
    }
}
