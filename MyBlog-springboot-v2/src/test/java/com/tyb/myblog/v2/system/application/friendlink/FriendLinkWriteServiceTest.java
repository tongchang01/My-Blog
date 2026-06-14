package com.tyb.myblog.v2.system.application.friendlink;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.system.domain.friendlink.FriendLink;
import com.tyb.myblog.v2.system.domain.friendlink.FriendLinkRepository;
import com.tyb.myblog.v2.system.domain.friendlink.FriendLinkStatus;
import com.tyb.myblog.v2.system.domain.friendlink.NewFriendLink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendLinkWriteServiceTest {

    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 6, 14, 12, 0);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-14T03:00:00Z"),
            ZoneId.of("Asia/Tokyo"));

    @Mock
    private FriendLinkRepository repository;

    private FriendLinkCreateService createService;
    private FriendLinkUpdateService updateService;

    @BeforeEach
    void setUp() {
        FriendLinkAuthorization authorization =
                new FriendLinkAuthorization();
        createService = new FriendLinkCreateService(
                repository, authorization);
        updateService = new FriendLinkUpdateService(
                repository, authorization, CLOCK);
    }

    @Test
    void createsAsAdminAndAllowsDuplicateUrl() {
        when(repository.insert(any(NewFriendLink.class)))
                .thenReturn(friendLink(
                        20L, "New", FriendLinkStatus.VISIBLE));

        FriendLinkResult result = createService.create(
                principal("1001", "ADMIN"),
                createCommand("https://example.com"));

        assertThat(result.id()).isEqualTo(20L);
        verify(repository).insert(argThat(link ->
                "New".equals(link.name())
                        && "https://example.com".equals(link.url())
                        && link.createdBy() == 1001L));
    }

    @Test
    void rejectsInvalidCreateInputsBeforeInsert() {
        assertError(
                () -> createService.create(
                        principal("1001", "DEMO"),
                        createCommand("https://example.com")),
                ApiErrorCode.FORBIDDEN);
        assertError(
                () -> createService.create(
                        principal("invalid", "ADMIN"),
                        createCommand("https://example.com")),
                ApiErrorCode.INVALID_TOKEN);
        assertError(
                () -> createService.create(
                        principal("0", "ADMIN"),
                        createCommand("https://example.com")),
                ApiErrorCode.INVALID_TOKEN);
        assertError(
                () -> createService.create(
                        principal("1001", "ADMIN"), null),
                ApiErrorCode.VALIDATION_ERROR);
        assertError(
                () -> createService.create(
                        principal("1001", "ADMIN"),
                        createCommand("ftp://example.com")),
                ApiErrorCode.VALIDATION_ERROR);

        verify(repository, never()).insert(any());
    }

    @Test
    void updatesLockedRowAndReturnsRereadState() {
        when(repository.findActiveByIdForUpdate(10L))
                .thenReturn(Optional.of(friendLink(
                        10L, "Old", FriendLinkStatus.VISIBLE)));
        when(repository.update(any(), eq(NOW), eq(1001L)))
                .thenReturn(true);
        when(repository.findActiveById(10L))
                .thenReturn(Optional.of(friendLink(
                        10L, "New", FriendLinkStatus.HIDDEN)));

        FriendLinkResult result = updateService.update(
                principal("1001", "ADMIN"),
                10L,
                updateCommand());

        assertThat(result.name()).isEqualTo("New");
        verify(repository).findActiveByIdForUpdate(10L);
        verify(repository).update(
                argThat(link ->
                        link.id() == 10L
                                && "New".equals(link.name())
                                && link.avatarUrl() == null
                                && link.description() == null
                                && link.sortOrder() == 30
                                && link.status()
                                == FriendLinkStatus.HIDDEN),
                eq(NOW),
                eq(1001L));
    }

    @Test
    void rejectsInvalidOrMissingUpdateTarget() {
        assertError(
                () -> updateService.update(
                        principal("1001", "ADMIN"),
                        0,
                        updateCommand()),
                ApiErrorCode.VALIDATION_ERROR);
        assertError(
                () -> updateService.update(
                        principal("1001", "ADMIN"),
                        10L,
                        null),
                ApiErrorCode.VALIDATION_ERROR);
        when(repository.findActiveByIdForUpdate(10L))
                .thenReturn(Optional.empty());
        assertError(
                () -> updateService.update(
                        principal("1001", "ADMIN"),
                        10L,
                        updateCommand()),
                ApiErrorCode.NOT_FOUND);
    }

    @Test
    void reportsAbnormalUpdateAndMissingReread() {
        when(repository.findActiveByIdForUpdate(10L))
                .thenReturn(Optional.of(friendLink(
                        10L, "Old", FriendLinkStatus.VISIBLE)));
        when(repository.update(any(), eq(NOW), eq(1001L)))
                .thenReturn(false);
        assertError(
                () -> updateService.update(
                        principal("1001", "ADMIN"),
                        10L,
                        updateCommand()),
                ApiErrorCode.INTERNAL_ERROR);

        when(repository.update(any(), eq(NOW), eq(1001L)))
                .thenReturn(true);
        when(repository.findActiveById(10L))
                .thenReturn(Optional.empty());
        assertError(
                () -> updateService.update(
                        principal("1001", "ADMIN"),
                        10L,
                        updateCommand()),
                ApiErrorCode.INTERNAL_ERROR);
    }

    private void assertError(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable callable,
            ApiErrorCode code) {
        assertThatThrownBy(callable)
                .isInstanceOfSatisfying(
                        ApiException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(code));
    }

    private AuthenticatedPrincipal principal(String id, String role) {
        return new AuthenticatedPrincipal(
                id, role.toLowerCase(), List.of(role));
    }

    private CreateFriendLinkCommand createCommand(String url) {
        return new CreateFriendLinkCommand(
                "New", url, null, null,
                30, FriendLinkStatus.VISIBLE);
    }

    private UpdateFriendLinkCommand updateCommand() {
        return new UpdateFriendLinkCommand(
                "New",
                "https://new.example.com",
                null,
                null,
                30,
                FriendLinkStatus.HIDDEN);
    }

    private FriendLink friendLink(
            long id,
            String name,
            FriendLinkStatus status) {
        return FriendLink.reconstitute(
                id,
                name,
                "https://example.com",
                null,
                null,
                10,
                status,
                LocalDateTime.of(2026, 6, 1, 0, 0),
                900L,
                NOW,
                1001L);
    }
}
