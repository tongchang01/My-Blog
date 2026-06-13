package com.tyb.myblog.v2.identity.application.profile;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.identity.domain.account.AccountType;
import com.tyb.myblog.v2.identity.domain.account.CurrentAccount;
import com.tyb.myblog.v2.identity.domain.account.CurrentAccountRepository;
import com.tyb.myblog.v2.identity.domain.profile.UserProfile;
import com.tyb.myblog.v2.identity.domain.profile.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 当前用户资料查询应用服务单元测试。
 */
class CurrentUserProfileQueryServiceTest {

    private final CurrentAccountRepository accountRepository =
            mock(CurrentAccountRepository.class);
    private final UserProfileRepository profileRepository =
            mock(UserProfileRepository.class);
    private final CurrentUserProfileQueryService service =
            new CurrentUserProfileQueryService(accountRepository, profileRepository);

    @Test
    void shouldCombineAccountAndProfile() {
        UserProfile profile = createProfile(1001L);
        when(accountRepository.findActiveById(1001L))
                .thenReturn(Optional.of(
                        new CurrentAccount(1001L, "admin", AccountType.ADMIN)));
        when(profileRepository.findActiveByUserId(1001L))
                .thenReturn(Optional.of(profile));

        CurrentUserProfileResult result = service.query("1001");

        assertThat(result.id()).isEqualTo(1001L);
        assertThat(result.username()).isEqualTo("admin");
        assertThat(result.type()).isEqualTo(AccountType.ADMIN);
        assertThat(result.profile()).isEqualTo(profile);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "abc", "0", "-1"})
    void shouldRejectInvalidPrincipalId(String principalId) {
        assertErrorCode(
                () -> service.query(principalId),
                ApiErrorCode.INVALID_TOKEN);

        verifyNoInteractions(accountRepository, profileRepository);
    }

    @Test
    void shouldReturnInternalErrorWhenAccountIsMissing() {
        UserProfile profile = createProfile(1001L);
        when(accountRepository.findActiveById(1001L))
                .thenReturn(Optional.empty());
        when(profileRepository.findActiveByUserId(1001L))
                .thenReturn(Optional.of(profile));

        assertErrorCode(
                () -> service.query("1001"),
                ApiErrorCode.INTERNAL_ERROR);
    }

    @Test
    void shouldReturnInternalErrorWhenProfileIsMissing() {
        when(accountRepository.findActiveById(1001L))
                .thenReturn(Optional.of(
                        new CurrentAccount(1001L, "admin", AccountType.ADMIN)));
        when(profileRepository.findActiveByUserId(1001L))
                .thenReturn(Optional.empty());

        assertErrorCode(
                () -> service.query("1001"),
                ApiErrorCode.INTERNAL_ERROR);
    }

    private UserProfile createProfile(long userId) {
        return UserProfile.create(
                userId,
                "TYB",
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
                null);
    }

    private void assertErrorCode(
            Runnable invocation,
            ApiErrorCode expectedCode
    ) {
        assertThatThrownBy(invocation::run)
                .isInstanceOfSatisfying(
                        ApiException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(expectedCode));
    }
}
