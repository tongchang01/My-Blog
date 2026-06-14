package com.tyb.myblog.v2.system.application.friendlink;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.system.domain.friendlink.FriendLink;
import com.tyb.myblog.v2.system.domain.friendlink.FriendLinkRepository;
import com.tyb.myblog.v2.system.domain.friendlink.FriendLinkStatus;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendLinkSortDeleteServiceTest {

    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 6, 14, 12, 0);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-14T03:00:00Z"),
            ZoneId.of("Asia/Tokyo"));

    @Mock
    private FriendLinkRepository repository;

    private FriendLinkStatusService statusService;
    private FriendLinkSortService sortService;
    private FriendLinkDeleteService deleteService;

    @BeforeEach
    void setUp() {
        FriendLinkAuthorization authorization =
                new FriendLinkAuthorization();
        statusService = new FriendLinkStatusService(
                repository, authorization, CLOCK);
        sortService = new FriendLinkSortService(
                repository, authorization, CLOCK);
        deleteService = new FriendLinkDeleteService(
                repository, authorization, CLOCK);
    }

    @Test
    void updatesStatusAfterLockAndRereads() {
        when(repository.findActiveByIdForUpdate(10L))
                .thenReturn(Optional.of(link(
                        10L, 0, FriendLinkStatus.VISIBLE)));
        when(repository.updateStatus(
                10L, FriendLinkStatus.HIDDEN, NOW, 1001L))
                .thenReturn(true);
        when(repository.findActiveById(10L))
                .thenReturn(Optional.of(link(
                        10L, 0, FriendLinkStatus.HIDDEN)));

        FriendLinkResult result = statusService.update(
                principal("ADMIN"),
                10L,
                new UpdateFriendLinkStatusCommand(
                        FriendLinkStatus.HIDDEN));

        assertThat(result.status()).isEqualTo(FriendLinkStatus.HIDDEN);
        var order = inOrder(repository);
        order.verify(repository).findActiveByIdForUpdate(10L);
        order.verify(repository).updateStatus(
                10L, FriendLinkStatus.HIDDEN, NOW, 1001L);
        order.verify(repository).findActiveById(10L);
    }

    @Test
    void validatesStatusAndMissingTarget() {
        assertError(
                () -> statusService.update(
                        principal("DEMO"),
                        10L,
                        new UpdateFriendLinkStatusCommand(
                                FriendLinkStatus.HIDDEN)),
                ApiErrorCode.FORBIDDEN);
        assertError(
                () -> statusService.update(
                        principal("ADMIN"), 0,
                        new UpdateFriendLinkStatusCommand(
                                FriendLinkStatus.HIDDEN)),
                ApiErrorCode.VALIDATION_ERROR);
        assertError(
                () -> statusService.update(
                        principal("ADMIN"), 10L, null),
                ApiErrorCode.VALIDATION_ERROR);
        assertError(
                () -> statusService.update(
                        principal("ADMIN"), 10L,
                        new UpdateFriendLinkStatusCommand(null)),
                ApiErrorCode.VALIDATION_ERROR);
        when(repository.findActiveByIdForUpdate(10L))
                .thenReturn(Optional.empty());
        assertError(
                () -> statusService.update(
                        principal("ADMIN"), 10L,
                        new UpdateFriendLinkStatusCommand(
                                FriendLinkStatus.HIDDEN)),
                ApiErrorCode.NOT_FOUND);
    }

    @Test
    void sortsPartialListAfterLockingIdsInAscendingOrder() {
        List<FriendLinkSortItem> items = List.of(
                new FriendLinkSortItem(20L, 1),
                new FriendLinkSortItem(10L, 2));
        when(repository.findActiveByIdsForUpdate(
                List.of(10L, 20L)))
                .thenReturn(List.of(
                        link(10L, 0, FriendLinkStatus.VISIBLE),
                        link(20L, 0, FriendLinkStatus.VISIBLE)));
        when(repository.updateSortOrder(any(Long.class), any(Integer.class),
                eq(NOW), eq(1001L))).thenReturn(true);
        when(repository.findActiveById(20L))
                .thenReturn(Optional.of(link(
                        20L, 1, FriendLinkStatus.VISIBLE)));
        when(repository.findActiveById(10L))
                .thenReturn(Optional.of(link(
                        10L, 2, FriendLinkStatus.VISIBLE)));

        List<FriendLinkResult> results = sortService.update(
                principal("ADMIN"),
                new UpdateFriendLinkSortOrdersCommand(items));

        assertThat(results).extracting(FriendLinkResult::id)
                .containsExactly(20L, 10L);
        verify(repository).findActiveByIdsForUpdate(
                List.of(10L, 20L));
        verify(repository).updateSortOrder(20L, 1, NOW, 1001L);
        verify(repository).updateSortOrder(10L, 2, NOW, 1001L);
    }

    @Test
    void rejectsInvalidSortRequestsBeforeUpdate() {
        assertSortError(null);
        assertSortError(List.of());
        assertSortError(java.util.stream.IntStream.rangeClosed(1, 101)
                .mapToObj(id -> new FriendLinkSortItem(id, id))
                .toList());
        assertSortError(List.of(new FriendLinkSortItem(0, 1)));
        assertSortError(List.of(new FriendLinkSortItem(1, -1)));
        assertSortError(List.of(
                new FriendLinkSortItem(1, 1),
                new FriendLinkSortItem(1, 2)));

        verify(repository, never()).updateSortOrder(
                any(Long.class), any(Integer.class),
                any(), any(Long.class));
    }

    @Test
    void rollsBackSortWhenAnyTargetIsMissingOrUpdateFails() {
        List<FriendLinkSortItem> items = List.of(
                new FriendLinkSortItem(10L, 1),
                new FriendLinkSortItem(20L, 2));
        when(repository.findActiveByIdsForUpdate(
                List.of(10L, 20L)))
                .thenReturn(List.of(
                        link(10L, 0, FriendLinkStatus.VISIBLE)));
        assertError(
                () -> sortService.update(
                        principal("ADMIN"),
                        new UpdateFriendLinkSortOrdersCommand(items)),
                ApiErrorCode.NOT_FOUND);
        verify(repository, never()).updateSortOrder(
                any(Long.class), any(Integer.class),
                any(), any(Long.class));

        when(repository.findActiveByIdsForUpdate(
                List.of(10L, 20L)))
                .thenReturn(List.of(
                        link(10L, 0, FriendLinkStatus.VISIBLE),
                        link(20L, 0, FriendLinkStatus.VISIBLE)));
        when(repository.updateSortOrder(10L, 1, NOW, 1001L))
                .thenReturn(false);
        assertError(
                () -> sortService.update(
                        principal("ADMIN"),
                        new UpdateFriendLinkSortOrdersCommand(items)),
                ApiErrorCode.INTERNAL_ERROR);
    }

    @Test
    void softDeletesLockedActiveRow() {
        when(repository.findActiveByIdForUpdate(10L))
                .thenReturn(Optional.of(link(
                        10L, 0, FriendLinkStatus.VISIBLE)));
        when(repository.softDelete(10L, NOW, 1001L))
                .thenReturn(true);

        deleteService.delete(principal("ADMIN"), 10L);

        verify(repository).findActiveByIdForUpdate(10L);
        verify(repository).softDelete(10L, NOW, 1001L);
    }

    @Test
    void reportsMissingAndAbnormalDelete() {
        when(repository.findActiveByIdForUpdate(10L))
                .thenReturn(Optional.empty());
        assertError(
                () -> deleteService.delete(
                        principal("ADMIN"), 10L),
                ApiErrorCode.NOT_FOUND);

        when(repository.findActiveByIdForUpdate(10L))
                .thenReturn(Optional.of(link(
                        10L, 0, FriendLinkStatus.VISIBLE)));
        when(repository.softDelete(10L, NOW, 1001L))
                .thenReturn(false);
        assertError(
                () -> deleteService.delete(
                        principal("ADMIN"), 10L),
                ApiErrorCode.INTERNAL_ERROR);
    }

    private void assertSortError(List<FriendLinkSortItem> items) {
        assertError(
                () -> sortService.update(
                        principal("ADMIN"),
                        new UpdateFriendLinkSortOrdersCommand(items)),
                ApiErrorCode.VALIDATION_ERROR);
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

    private AuthenticatedPrincipal principal(String role) {
        return new AuthenticatedPrincipal(
                "1001", role.toLowerCase(), List.of(role));
    }

    private FriendLink link(
            long id,
            int sortOrder,
            FriendLinkStatus status) {
        return FriendLink.reconstitute(
                id,
                "Link " + id,
                "https://example.com/" + id,
                null,
                null,
                sortOrder,
                status,
                NOW,
                1001L,
                NOW,
                1001L);
    }
}
