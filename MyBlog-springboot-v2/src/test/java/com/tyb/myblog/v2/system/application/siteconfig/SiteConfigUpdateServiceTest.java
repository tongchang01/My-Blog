package com.tyb.myblog.v2.system.application.siteconfig;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.system.domain.siteconfig.SiteConfig;
import com.tyb.myblog.v2.system.domain.siteconfig.SiteConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 站点配置全量更新服务测试。
 */
@ExtendWith(MockitoExtension.class)
class SiteConfigUpdateServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-14T03:00:00Z"),
            ZoneId.of("Asia/Tokyo"));

    @Mock
    private SiteConfigRepository repository;

    private SiteConfigUpdateService service;

    @BeforeEach
    void setUp() {
        service = new SiteConfigUpdateService(repository, CLOCK);
    }

    @Test
    void updatesAsAdminAndReturnsRereadDatabaseState() {
        SiteConfig current = current();
        SiteConfig persisted = updated(
                LocalDateTime.of(2026, 6, 14, 12, 0),
                1001L);
        when(repository.findActiveForUpdate())
                .thenReturn(Optional.of(current));
        when(repository.update(
                any(),
                eq(LocalDateTime.of(2026, 6, 14, 12, 0)),
                eq(1001L)))
                .thenReturn(true);
        when(repository.findActive()).thenReturn(Optional.of(persisted));

        AdminSiteConfigResult result =
                service.update(principal("1001", "ADMIN"), command());

        assertThat(result).isEqualTo(AdminSiteConfigResult.from(persisted));
        ArgumentCaptor<SiteConfig> configCaptor =
                ArgumentCaptor.forClass(SiteConfig.class);
        verify(repository).update(
                configCaptor.capture(),
                eq(LocalDateTime.of(2026, 6, 14, 12, 0)),
                eq(1001L));
        assertThat(configCaptor.getValue().siteTitleZh()).isEqualTo("新标题");
        assertThat(configCaptor.getValue().aboutMdZh())
                .isEqualTo("# 新的关于我\n\n正文");
    }

    @Test
    void rejectsDemoAndInvalidPrincipal() {
        assertError(
                () -> service.update(principal("1002", "DEMO"), command()),
                ApiErrorCode.FORBIDDEN);
        assertError(
                () -> service.update(null, command()),
                ApiErrorCode.INVALID_TOKEN);
        assertError(
                () -> service.update(principal("invalid", "ADMIN"), command()),
                ApiErrorCode.INVALID_TOKEN);
        assertError(
                () -> service.update(principal("0", "ADMIN"), command()),
                ApiErrorCode.INVALID_TOKEN);
    }

    @Test
    void rejectsNullCommandAndInvalidDomainFields() {
        assertError(
                () -> service.update(principal("1001", "ADMIN"), null),
                ApiErrorCode.VALIDATION_ERROR);

        when(repository.findActiveForUpdate())
                .thenReturn(Optional.of(current()));
        UpdateSiteConfigCommand invalid = new UpdateSiteConfigCommand(
                null, null, null, null, null, null, null, null, null,
                null, null, null, null);
        assertError(
                () -> service.update(principal("1001", "ADMIN"), invalid),
                ApiErrorCode.VALIDATION_ERROR);
    }

    @Test
    void reportsMissingFixedRowAndAbnormalUpdateCount() {
        when(repository.findActiveForUpdate()).thenReturn(Optional.empty());
        assertError(
                () -> service.update(principal("1001", "ADMIN"), command()),
                ApiErrorCode.INTERNAL_ERROR);

        when(repository.findActiveForUpdate())
                .thenReturn(Optional.of(current()));
        when(repository.update(any(), any(), eq(1001L))).thenReturn(false);
        assertError(
                () -> service.update(principal("1001", "ADMIN"), command()),
                ApiErrorCode.INTERNAL_ERROR);
    }

    @Test
    void reportsMissingRereadAfterSuccessfulUpdate() {
        when(repository.findActiveForUpdate())
                .thenReturn(Optional.of(current()));
        when(repository.update(any(), any(), eq(1001L))).thenReturn(true);
        when(repository.findActive()).thenReturn(Optional.empty());

        assertError(
                () -> service.update(principal("1001", "ADMIN"), command()),
                ApiErrorCode.INTERNAL_ERROR);
    }

    private void assertError(Runnable action, ApiErrorCode code) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        ApiException.class,
                        exception -> assertThat(exception.code()).isEqualTo(code));
    }

    private AuthenticatedPrincipal principal(String id, String role) {
        return new AuthenticatedPrincipal(id, "admin", List.of(role));
    }

    private UpdateSiteConfigCommand command() {
        return new UpdateSiteConfigCommand(
                "新标题",
                null,
                "New title",
                "新副标题",
                null,
                null,
                "# 新的关于我\n\n正文",
                null,
                null,
                "https://example.com/new-logo.png",
                null,
                null,
                "new_playlist");
    }

    private SiteConfig current() {
        return SiteConfig.create(
                1L,
                "旧标题",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                900L);
    }

    private SiteConfig updated(LocalDateTime updatedAt, Long updatedBy) {
        return SiteConfig.create(
                1L,
                "新标题",
                null,
                "New title",
                "新副标题",
                null,
                null,
                "# 新的关于我\n\n正文",
                null,
                null,
                "https://example.com/new-logo.png",
                null,
                null,
                "new_playlist",
                updatedAt,
                updatedBy);
    }
}
