package com.tyb.myblog.v2.identity.application.token;

import com.tyb.myblog.v2.identity.domain.auth.UserTokenVersionRepository;
import com.tyb.myblog.v2.identity.domain.token.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 撤销指定用户的全部访问令牌和 refresh token。
 */
@Service
public class UserTokenRevocationService {

    private final UserTokenVersionRepository userTokenVersionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    public UserTokenRevocationService(
            UserTokenVersionRepository userTokenVersionRepository,
            RefreshTokenRepository refreshTokenRepository,
            Clock clock) {
        this.userTokenVersionRepository = userTokenVersionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.clock = clock;
    }

    /**
     * 撤销目标用户当前签发的全部 token。
     *
     * <p>先递增 access token 版本，再撤销全部 refresh token；两项操作处于同一事务。
     * {@code operatorUserId} 表示实际执行操作的后台用户，不能使用目标用户 ID 代替。</p>
     *
     * @param userId         被撤销 token 的用户 ID
     * @param operatorUserId 实际执行撤销操作的用户 ID
     * @return 用户存在且完成版本递增时返回 true
     */
    @Transactional
    public boolean revokeAll(long userId, Long operatorUserId) {
        LocalDateTime updatedAt = LocalDateTime.now(clock);
        if (!userTokenVersionRepository.incrementActiveTokenVersion(userId, updatedAt, operatorUserId)) {
            return false;
        }
        refreshTokenRepository.revokeAllByUserId(userId);
        return true;
    }
}
