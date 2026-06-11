package com.tyb.myblog.v2.identity.application.token;

import com.tyb.myblog.v2.identity.domain.auth.UserTokenVersionRepository;
import com.tyb.myblog.v2.identity.domain.token.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 撤销指定用户的全部访问令牌和 refresh token。
 */
@Service
public class UserTokenRevocationService {

    private final UserTokenVersionRepository userTokenVersionRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public UserTokenRevocationService(
            UserTokenVersionRepository userTokenVersionRepository,
            RefreshTokenRepository refreshTokenRepository) {
        this.userTokenVersionRepository = userTokenVersionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public boolean revokeAll(long userId) {
        if (!userTokenVersionRepository.incrementActiveTokenVersion(userId)) {
            return false;
        }
        refreshTokenRepository.revokeAllByUserId(userId);
        return true;
    }
}
