package com.tyb.myblog.v2.identity.application.profile;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.identity.domain.profile.UserProfile;
import com.tyb.myblog.v2.identity.domain.profile.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 当前用户资料更新应用服务测试。
 */
class CurrentUserProfileUpdateServiceTest {

    private final UserProfileRepository repository = mock(UserProfileRepository.class);
    private final CurrentUserProfileUpdateService service =
            new CurrentUserProfileUpdateService(repository);

    @Test
    void shouldRepresentAbsentAndExplicitNullPatchValues() {
        assertThat(PatchValue.<String>absent().present()).isFalse();
        assertThat(PatchValue.<String>absent().value()).isNull();
        assertThat(PatchValue.<String>of(null).present()).isTrue();
        assertThat(PatchValue.<String>of(null).value()).isNull();
    }

    @Test
    void shouldNormalizeNullWrappersToAbsent() {
        UpdateCurrentUserProfileCommand command = command(null, PatchValue.of("Tokyo"));

        assertThat(command.nickname().present()).isFalse();
        assertThat(command.location().present()).isTrue();
        assertThat(command.hasAnyPresentField()).isTrue();
    }

    @Test
    void shouldUpdateProfileForAdminAndReturnUpdatedProfile() {
        UserProfile current = profile("TYB", "Tokyo");
        UpdateCurrentUserProfileCommand command =
                command(PatchValue.of(" New Name "), PatchValue.of(null));
        when(repository.findActiveByUserIdForUpdate(1001L))
                .thenReturn(Optional.of(current));
        when(repository.update(org.mockito.ArgumentMatchers.any(UserProfile.class)))
                .thenReturn(true);

        UserProfileResult updated = service.update(admin("1001"), command);

        assertThat(updated.nickname()).isEqualTo("New Name");
        assertThat(updated.location()).isNull();
        verify(repository).update(
                org.mockito.ArgumentMatchers.any(UserProfile.class));
    }

    @Test
    void shouldRejectDemoBeforeRepositoryInteraction() {
        assertErrorCode(
                () -> service.update(demo("1001"),
                        command(PatchValue.of("New"), PatchValue.absent())),
                ApiErrorCode.FORBIDDEN);

        verifyNoInteractions(repository);
    }

    @Test
    void shouldRejectEmptyPatch() {
        assertErrorCode(
                () -> service.update(admin("1001"),
                        command(PatchValue.absent(), PatchValue.absent())),
                ApiErrorCode.VALIDATION_ERROR);

        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "abc", "0", "-1"})
    void shouldRejectInvalidPrincipal(String principalId) {
        assertErrorCode(
                () -> service.update(admin(principalId),
                        command(PatchValue.of("New"), PatchValue.absent())),
                ApiErrorCode.INVALID_TOKEN);

        verifyNoInteractions(repository);
    }

    @Test
    void shouldReturnInternalErrorWhenProfileIsMissing() {
        when(repository.findActiveByUserIdForUpdate(1001L))
                .thenReturn(Optional.empty());

        assertErrorCode(
                () -> service.update(admin("1001"),
                        command(PatchValue.of("New"), PatchValue.absent())),
                ApiErrorCode.INTERNAL_ERROR);
    }

    @Test
    void shouldMapDomainValidationToValidationError() {
        when(repository.findActiveByUserIdForUpdate(1001L))
                .thenReturn(Optional.of(profile("TYB", "Tokyo")));

        assertThatThrownBy(() -> service.update(admin("1001"),
                command(PatchValue.of(" "), PatchValue.absent())))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.code()).isEqualTo(ApiErrorCode.VALIDATION_ERROR);
                    assertThat(exception.getMessage()).isEqualTo("昵称不能为空");
                });
    }

    @Test
    void shouldReturnInternalErrorWhenUpdateAffectsUnexpectedRows() {
        when(repository.findActiveByUserIdForUpdate(1001L))
                .thenReturn(Optional.of(profile("TYB", "Tokyo")));
        when(repository.update(org.mockito.ArgumentMatchers.any(UserProfile.class)))
                .thenReturn(false);

        assertErrorCode(
                () -> service.update(admin("1001"),
                        command(PatchValue.of("New"), PatchValue.absent())),
                ApiErrorCode.INTERNAL_ERROR);
    }

    private UpdateCurrentUserProfileCommand command(
            PatchValue<String> nickname,
            PatchValue<String> location) {
        PatchValue<String> absent = PatchValue.absent();
        return new UpdateCurrentUserProfileCommand(
                nickname, absent, absent, absent, absent, location, absent,
                absent, absent, absent, absent, absent, absent, absent);
    }

    private UserProfile profile(String nickname, String location) {
        return UserProfile.create(
                1001L, nickname, null, null, null, null, location,
                null, null, null, null, null, null, null, null);
    }

    private AuthenticatedPrincipal admin(String id) {
        return new AuthenticatedPrincipal(id, "admin", List.of("ADMIN"));
    }

    private AuthenticatedPrincipal demo(String id) {
        return new AuthenticatedPrincipal(id, "demo", List.of("DEMO"));
    }

    private void assertErrorCode(Runnable invocation, ApiErrorCode expectedCode) {
        assertThatThrownBy(invocation::run)
                .isInstanceOfSatisfying(
                        ApiException.class,
                        exception -> assertThat(exception.code()).isEqualTo(expectedCode));
    }
}
