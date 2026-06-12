package com.tyb.myblog.v2.identity.domain.auth;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * 后台账号持久化锁定规则测试。
 */
class LoginLockPolicyTest {

    @Test
    void exposesValidatedThresholdAndDuration() {
        LoginLockPolicy policy = new LoginLockPolicy(5, Duration.ofMinutes(10));

        assertThat(policy.maxAttempts()).isEqualTo(5);
        assertThat(policy.lockDuration()).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    void rejectsInvalidThresholdOrDuration() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LoginLockPolicy(0, Duration.ofMinutes(10)));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LoginLockPolicy(5, Duration.ZERO));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LoginLockPolicy(5, Duration.ofMinutes(-1)));
    }
}
