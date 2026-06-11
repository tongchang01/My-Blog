package com.tyb.myblog.v2.identity.application;

import com.tyb.myblog.v2.identity.application.token.UserTokenRevocationService;
import com.tyb.myblog.v2.identity.domain.auth.UserTokenVersionRepository;
import com.tyb.myblog.v2.identity.domain.token.RefreshTokenRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserTokenRevocationServiceUnitTest {

    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    private static final Instant FIXED_INSTANT = Instant.parse("2026-06-11T12:00:00Z");

    /**
     * 验证整体撤销使用应用统一时钟，并将实际操作者写入审计字段。
     */
    @Test
    void recordsActualOperatorAndApplicationClockWhenRevokingTokens() {
        UserTokenVersionRepository userRepository = mock(UserTokenVersionRepository.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        Clock clock = Clock.fixed(FIXED_INSTANT, TOKYO);
        LocalDateTime expectedUpdatedAt = LocalDateTime.ofInstant(FIXED_INSTANT, TOKYO);
        when(userRepository.incrementActiveTokenVersion(1001L, expectedUpdatedAt, 9001L))
                .thenReturn(true);
        UserTokenRevocationService service = new UserTokenRevocationService(
                userRepository,
                refreshTokenRepository,
                clock);

        boolean revoked = service.revokeAll(1001L, 9001L);

        assertThat(revoked).isTrue();
        verify(userRepository).incrementActiveTokenVersion(1001L, expectedUpdatedAt, 9001L);
        verify(refreshTokenRepository).revokeAllByUserId(1001L);
    }
}
