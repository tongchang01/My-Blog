package com.tyb.myblog.v2.identity.application.token;

import com.tyb.myblog.v2.common.config.SecurityJwtProperties;
import com.tyb.myblog.v2.identity.domain.auth.UserTokenVersionRepository;
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
 * refresh token 签发、轮换与撤销服务。
 */
@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository repository;
    private final UserTokenVersionRepository userTokenVersionRepository;
    private final SecurityJwtProperties properties;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository repository,
            UserTokenVersionRepository userTokenVersionRepository,
            SecurityJwtProperties properties,
            Clock clock) {
        this.repository = repository;
        this.userTokenVersionRepository = userTokenVersionRepository;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * 为指定后台用户签发新的 refresh token。
     *
     * <p>仅向调用方返回一次明文，持久层只保存 SHA-256 摘要；过期时间使用应用统一时钟计算。</p>
     *
     * @param userId 后台用户 ID
     * @return 包含明文和过期时间的签发结果
     */
    @Transactional
    public IssuedRefreshToken issue(long userId) {
        String rawToken = generateToken();
        LocalDateTime expiresAt = LocalDateTime.now(clock).plus(properties.refreshTokenTtl());
        repository.save(RefreshTokenRecord.active(userId, hash(rawToken), expiresAt));
        return new IssuedRefreshToken(userId, rawToken, expiresAt);
    }

    /**
     * 轮换仍有效的 refresh token。
     *
     * <p>查询时锁定旧记录，并在同一事务中完成撤销和重新签发，防止旧 token 被并发重复消费。</p>
     *
     * @param rawToken 调用方持有的 refresh token 明文
     * @return 轮换成功时返回新 token；token 无效或后台用户不可用时返回空
     */
    @Transactional
    public Optional<IssuedRefreshToken> rotate(String rawToken) {
        LocalDateTime now = LocalDateTime.now(clock);
        return repository.findActiveForUpdate(hash(rawToken), now)
                .filter(token -> repository.revoke(token.id()))
                .filter(token -> userTokenVersionRepository
                        .findRefreshableTokenVersion(token.userId(), now)
                        .isPresent())
                .map(token -> issue(token.userId()));
    }

    /**
     * 撤销一枚仍有效的 refresh token。
     *
     * @param rawToken 调用方持有的 refresh token 明文
     * @return 实际撤销成功时返回 true
     */
    @Transactional
    public boolean revoke(String rawToken) {
        return repository.findActiveForUpdate(hash(rawToken), LocalDateTime.now(clock))
                .map(token -> repository.revoke(token.id()))
                .orElse(false);
    }

    /**
     * 撤销指定后台用户的全部有效 refresh token。
     *
     * @param userId 后台用户 ID
     * @return 实际撤销的记录数
     */
    @Transactional
    public int revokeAllForUser(long userId) {
        return repository.revokeAllByUserId(userId);
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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
