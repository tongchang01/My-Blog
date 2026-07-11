package com.tyb.myblog.v2.identity.application.bootstrap;

import com.tyb.myblog.v2.common.config.BootstrapAdminProperties;
import com.tyb.myblog.v2.identity.domain.account.AccountType;
import com.tyb.myblog.v2.identity.domain.account.UserAccount;
import com.tyb.myblog.v2.identity.domain.auth.PasswordHashService;
import com.tyb.myblog.v2.identity.domain.bootstrap.AdminBootstrapRepository;
import com.tyb.myblog.v2.identity.domain.profile.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BootstrapAdminApplicationServiceTest {

    @Mock
    private AdminBootstrapRepository adminBootstrapRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private PasswordHashService passwordHashService;

    @InjectMocks
    private BootstrapAdminApplicationService service;

    @Test
    void createsAdminAndProfileWhenNoAdminExists() {
        BootstrapAdminProperties properties = new BootstrapAdminProperties(
                true, "admin", "12345678", false);
        when(adminBootstrapRepository.existsActiveAdmin()).thenReturn(false);
        when(passwordHashService.encode("12345678")).thenReturn("password-hash");
        when(adminBootstrapRepository.createAdmin("admin", "password-hash"))
                .thenReturn(new UserAccount(
                        1001L,
                        "admin",
                        "password-hash",
                        AccountType.ADMIN,
                        0,
                        0,
                        null));

        assertThat(service.bootstrap(properties)).isTrue();

        ArgumentCaptor<com.tyb.myblog.v2.identity.domain.profile.UserProfile> profile =
                ArgumentCaptor.forClass(
                        com.tyb.myblog.v2.identity.domain.profile.UserProfile.class);
        verify(userProfileRepository).insert(profile.capture());
        assertThat(profile.getValue().userId()).isEqualTo(1001L);
        assertThat(profile.getValue().nickname()).isEqualTo("admin");
    }

    @Test
    void skipsCreationWhenAdminAlreadyExists() {
        when(adminBootstrapRepository.existsActiveAdmin()).thenReturn(true);

        assertThat(service.bootstrap(new BootstrapAdminProperties(
                true, "admin", "12345678", false))).isFalse();

        verify(adminBootstrapRepository, never()).createAdmin(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
        verifyNoInteractions(userProfileRepository, passwordHashService);
    }
}
