package com.aurora.myblog.v2.modules.content.infrastructure;

import com.aurora.myblog.v2.common.config.SecurityJwtProperties;
import com.aurora.myblog.v2.modules.content.domain.ArticleAccessToken;
import com.aurora.myblog.v2.modules.content.domain.ArticleAccessTokenService;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
public class SignedArticleAccessTokenService implements ArticleAccessTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(30);

    private final byte[] secret;

    public SignedArticleAccessTokenService(SecurityJwtProperties jwtProperties) {
        this.secret = jwtProperties.secret().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public ArticleAccessToken issue(int articleId) {
        Instant expiresAt = Instant.now().plus(ACCESS_TOKEN_TTL);
        String payload = articleId + ":" + expiresAt.getEpochSecond();
        String signature = sign(payload);
        return new ArticleAccessToken(encode(payload) + "." + signature, expiresAt);
    }

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

    private String encode(String payload) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private boolean sameSignature(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
