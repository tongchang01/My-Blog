package com.tyb.myblog.v2.stats.infrastructure.privacy;

import com.tyb.myblog.v2.stats.infrastructure.config.StatsProperties;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class HmacVisitorHashGeneratorTest {

    @Test
    void rotatesHashByDateAndClientWithoutExposingInputs() {
        HmacVisitorHashGenerator generator = new HmacVisitorHashGenerator(
                properties("stats-secret-stats-secret-1234567890"));

        String first = generator.hash(
                LocalDate.of(2026, 6, 18),
                "203.0.113.1",
                "JUnit");
        String same = generator.hash(
                LocalDate.of(2026, 6, 18),
                "203.0.113.1",
                "JUnit");
        String nextDay = generator.hash(
                LocalDate.of(2026, 6, 19),
                "203.0.113.1",
                "JUnit");
        String nextClient = generator.hash(
                LocalDate.of(2026, 6, 18),
                "203.0.113.2",
                "JUnit");

        assertThat(first).matches("[0-9a-f]{64}").isEqualTo(same);
        assertThat(nextDay).isNotEqualTo(first);
        assertThat(nextClient).isNotEqualTo(first);
        assertThat(first).doesNotContain("203.0.113.1", "JUnit");
    }

    private StatsProperties properties(String secret) {
        return new StatsProperties(
                secret,
                120,
                20_000,
                new StatsProperties.Scheduling(
                        false,
                        "0 */5 * * * *",
                        "0 30 3 * * *"));
    }
}
