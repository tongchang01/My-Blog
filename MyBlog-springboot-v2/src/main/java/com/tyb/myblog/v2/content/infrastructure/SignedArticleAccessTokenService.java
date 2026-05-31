package com.tyb.myblog.v2.content.infrastructure;

import com.tyb.myblog.v2.common.config.SecurityJwtProperties;
import com.tyb.myblog.v2.content.domain.ArticleAccessToken;
import com.tyb.myblog.v2.content.domain.ArticleAccessTokenService;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
/**
 * 基于 HMAC 签名的受保护文章访问令牌服务。
 *
 * <p>令牌只用于访问单篇受保护文章，默认 30 分钟有效。
 * 签名密钥复用 JWT 密钥，后续如果文章访问策略变复杂，应拆分独立密钥。</p>
 */
public class SignedArticleAccessTokenService implements ArticleAccessTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(30);

    private final byte[] secret;

    public SignedArticleAccessTokenService(SecurityJwtProperties jwtProperties) {
        this.secret = jwtProperties.secret().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 签发指定文章的临时访问令牌。
     */
    @Override
    public ArticleAccessToken issue(int articleId) {
        Instant expiresAt = Instant.now().plus(ACCESS_TOKEN_TTL);
        String payload = articleId + ":" + expiresAt.getEpochSecond();
        String signature = sign(payload);
        return new ArticleAccessToken(encode(payload) + "." + signature, expiresAt);
    }

    /**
     * 校验访问令牌是否匹配当前文章且未过期。
     */
    @Override
    public boolean verify(int articleId, String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != 2) {
            return false;
        }
        String payload = decode(parts[0]);
        String[] payloadParts = payload.split(":", -1);
        if (payloadParts.length != 2) {
            return false;
        }
        try {
            int tokenArticleId = Integer.parseInt(payloadParts[0]);
            Instant expiresAt = Instant.ofEpochSecond(Long.parseLong(payloadParts[1]));
            return tokenArticleId == articleId
                    && Instant.now().isBefore(expiresAt)
                    && sameSignature(sign(payload), parts[1]);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /**
     * 对令牌载荷生成 HMAC 签名。
     */
    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot sign article access token", ex);
        }
    }

    /**
     * URL 安全 Base64 编码。
     */
    private String encode(String payload) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * URL 安全 Base64 解码。
     */
    private String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    /**
     * 常量时间比较签名，降低时序侧信道风险。
     */
    private boolean sameSignature(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
