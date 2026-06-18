package com.tyb.myblog.v2.stats.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DailyPageViewTest {

    @Test
    void rejectsUvGreaterThanPv() {
        assertThatThrownBy(() -> new DailyPageView(
                100L,
                StatsLanguage.ZH,
                LocalDate.of(2026, 6, 18),
                1,
                2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UV");
    }
}
