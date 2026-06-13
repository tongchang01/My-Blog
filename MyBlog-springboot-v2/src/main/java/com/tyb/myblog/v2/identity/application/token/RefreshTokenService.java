package com.tyb.myblog.v2.identity.application.token;

import com.tyb.myblog.v2.common.config.SecurityJwtProperties;
import com.tyb.myblog.v2.identity.domain.token.RefreshTokenRecord;
import com.tyb.myblog.v2.identity.domain.token.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * refresh token 的签发、锁定查询与撤销服务。
 */
@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository repository;
    private final SecurityJwtProperties properties;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository repository,
            SecurityJwtProperties properties,
            Clock clock
    ) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * 为指定后台账号签发新的 refresh token。
     *
     * <p>明文只返回一次，持久化层只保存 SHA-256 摘要。</p>
     */
    @Transactional
    public IssuedRefreshToken issue(long userId) {
        String rawToken = generateToken();
        LocalDateTime expiresAt =
                LocalDateTime.now(clock).plus(properties.refreshTokenTtl());
        repository.save(RefreshTokenRecord.active(
                userId,
                hash(rawToken),
                expiresAt));
        return new IssuedRefreshToken(userId, rawToken, expiresAt);
    }

    /**
     * 锁定仍有效的 refresh token，供外层轮换事务消费。
     */
    public Optional<RefreshTokenRecord> findActiveForUpdate(
            String rawToken,
            LocalDateTime now
    ) {
        return repository.findActiveForUpdate(hash(rawToken), now);
    }

    /**
     * 按主键撤销 refresh token，由外层事务决定提交或回滚。
     */
    public boolean revoke(long tokenId) {
        return repository.revoke(tokenId);
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("当前 JVM 不支持 SHA-256", ex);
        }
    }
}
