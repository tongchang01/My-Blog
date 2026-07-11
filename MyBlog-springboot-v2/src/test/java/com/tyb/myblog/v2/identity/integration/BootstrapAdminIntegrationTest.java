package com.tyb.myblog.v2.identity.integration;

import com.tyb.myblog.v2.identity.domain.account.UserAccountRepository;
import com.tyb.myblog.v2.identity.domain.profile.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "myblog.bootstrap-admin.enabled=true",
        "myblog.bootstrap-admin.username=admin",
        "myblog.bootstrap-admin.password=12345678"
})
@DirtiesContext
class BootstrapAdminIntegrationTest {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void createsAdminAndProfileFromExplicitBootstrapConfiguration() {
        var account = userAccountRepository.findActiveByUsername("admin")
                .orElseThrow();

        assertThat(account.canLoginToAdmin()).isTrue();
        assertThat(passwordEncoder.matches(
                "12345678", account.passwordHash())).isTrue();
        assertThat(userProfileRepository.findActiveByUserId(account.id()))
                .isPresent()
                .get()
                .extracting(profile -> profile.nickname())
                .isEqualTo("admin");
    }
}
