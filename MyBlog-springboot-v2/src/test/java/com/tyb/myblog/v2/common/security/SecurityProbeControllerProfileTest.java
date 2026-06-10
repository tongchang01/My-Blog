package com.tyb.myblog.v2.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityProbeControllerProfileTest {

    @Test
    void registersProbeForTestProfile() {
        assertThat(hasProbeController("test")).isTrue();
    }

    @Test
    void registersProbeForLocalProfile() {
        assertThat(hasProbeController("local")).isTrue();
    }

    @Test
    void doesNotRegisterProbeForProdProfile() {
        assertThat(hasProbeController("prod")).isFalse();
    }

    @Test
    void doesNotRegisterProbeWithoutExplicitProfile() {
        assertThat(hasProbeController()).isFalse();
    }

    private boolean hasProbeController(String... profiles) {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles(profiles);
            context.register(SecurityProbeController.class);
            context.refresh();
            return context.containsBean("securityProbeController");
        }
    }
}
