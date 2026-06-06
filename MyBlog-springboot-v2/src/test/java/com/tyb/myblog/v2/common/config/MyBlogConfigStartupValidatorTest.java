package com.tyb.myblog.v2.common.config;

import org.junit.jupiter.api.Test;

import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MyBlogConfigStartupValidatorTest {

    @Test
    void acceptsAsiaTokyoJvmTimezone() {
        withDefaultTimeZone("Asia/Tokyo", () ->
                assertThatCode(new MyBlogConfigStartupValidator()::afterPropertiesSet)
                        .doesNotThrowAnyException());
    }

    @Test
    void rejectsOtherJvmTimezone() {
        withDefaultTimeZone("UTC", () ->
                assertThatThrownBy(new MyBlogConfigStartupValidator()::afterPropertiesSet)
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("Asia/Tokyo")
                        .hasMessageContaining("-Duser.timezone"));
    }

    private void withDefaultTimeZone(String zoneId, Runnable assertion) {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
            assertion.run();
        } finally {
            TimeZone.setDefault(original);
        }
    }
}
