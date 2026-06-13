package com.tyb.myblog.v2.identity.application;

import com.tyb.myblog.v2.identity.application.token.IssuedRefreshToken;
import com.tyb.myblog.v2.identity.application.token.RefreshTokenService;
import com.tyb.myblog.v2.identity.domain.token.RefreshTokenRecord;
import com.tyb.myblog.v2.identity.domain.token.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.Optional;

import com.tyb.myblog.v2.common.config.SecurityJwtProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * refresh token 基础能力单元测试。
 */
class RefreshTokenServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-13T01:00:00Z"),
            ZoneId.of("Asia/Tokyo"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    private final RefreshTokenRepository repository =
            mock(RefreshTokenRepository.class);
    private final RefreshTokenService service = new RefreshTokenService(
            repository,
            new SecurityJwtProperties(
                    "myblog-v2-test",
                    "test-secret-test-secret-test-secret-123456",
                    Duration.ofMinutes(15),
                    Duration.ofDays(7)),
            CLOCK);

    @Test
    void shouldStoreOnlySha256HashWhenIssuingToken() {
        ArgumentCaptor<RefreshTokenRecord> captor =
                ArgumentCaptor.forClass(RefreshTokenRecord.class);

        IssuedRefreshToken issued = service.issue(1001L);

        verify(repository).save(captor.capture());
        RefreshTokenRecord stored = captor.getValue();
        assertThat(stored.tokenHash()).isEqualTo(sha256(issued.token()));
        assertThat(stored.tokenHash()).isNotEqualTo(issued.token());
        assertThat(stored.expiresAt()).isEqualTo(NOW.plusDays(7));
    }

    @Test
    void shouldHashRawTokenWhenLockingActiveRecord() {
        RefreshTokenRecord record = new RefreshTokenRecord(
                10L, 1001L, sha256("raw-token"), NOW.plusDays(1), false);
        when(repository.findActiveForUpdate(sha256("raw-token"), NOW))
                .thenReturn(Optional.of(record));

        Optional<RefreshTokenRecord> result =
                service.findActiveForUpdate("raw-token", NOW);

        assertThat(result).contains(record);
    }

    @Test
    void shouldReturnEmptyWhenActiveRecordDoesNotExist() {
        when(repository.findActiveForUpdate(sha256("missing"), NOW))
                .thenReturn(Optional.empty());

        assertThat(service.findActiveForUpdate("missing", NOW)).isEmpty();
    }

    @Test
    void shouldRevokeRecordByPrimaryKey() {
        when(repository.revoke(10L)).thenReturn(true);

        assertThat(service.revoke(10L)).isTrue();
        verify(repository).revoke(10L);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
