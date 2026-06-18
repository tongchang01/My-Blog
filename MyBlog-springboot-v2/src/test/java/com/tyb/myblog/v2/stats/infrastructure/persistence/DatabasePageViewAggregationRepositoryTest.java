package com.tyb.myblog.v2.stats.infrastructure.persistence;

import com.tyb.myblog.v2.stats.domain.DailyPageView;
import com.tyb.myblog.v2.stats.domain.PageViewAggregationRepository;
import com.tyb.myblog.v2.stats.domain.StatsLanguage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class DatabasePageViewAggregationRepositoryTest {

    private static final LocalDate DATE =
            LocalDate.of(2026, 6, 18);

    @Autowired
    private PageViewAggregationRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetState() {
        jdbcTemplate.update("DELETE FROM t_page_view_daily");
        jdbcTemplate.update("DELETE FROM t_page_view");
    }

    @Test
    void summarizesPvUvAndGeneralPages() {
        insert(100L, "zh", "a".repeat(64), "2026-06-18T01:00:00");
        insert(100L, "zh", "a".repeat(64), "2026-06-18T02:00:00");
        insert(100L, "zh", "b".repeat(64), "2026-06-18T03:00:00");
        insert(null, "en", "c".repeat(64), "2026-06-18T04:00:00");
        insert(100L, "zh", "d".repeat(64), "2026-06-19T00:00:00");

        assertThat(repository.summarize(
                DATE.atStartOfDay(),
                DATE.plusDays(1).atStartOfDay(),
                DATE))
                .containsExactlyInAnyOrder(
                        new DailyPageView(
                                100L,
                                StatsLanguage.ZH,
                                DATE,
                                3,
                                2),
                        new DailyPageView(
                                0L,
                                StatsLanguage.EN,
                                DATE,
                                1,
                                1));
    }

    @Test
    void replacesDayIdempotentlyAndCleansOnlyOlderRows() {
        List<DailyPageView> rows = List.of(new DailyPageView(
                100L,
                StatsLanguage.ZH,
                DATE,
                3,
                2));
        repository.deleteDay(DATE);
        repository.insertAll(rows);
        repository.deleteDay(DATE);
        repository.insertAll(rows);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_page_view_daily",
                Integer.class)).isEqualTo(1);

        insert(100L, "zh", "a".repeat(64), "2026-03-20T23:59:59");
        insert(100L, "zh", "b".repeat(64), "2026-03-21T00:00:00");
        assertThat(repository.deleteRawBefore(
                LocalDateTime.of(2026, 3, 21, 0, 0)))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_page_view",
                Integer.class)).isEqualTo(1);
    }

    private void insert(
            Long articleId,
            String lang,
            String hash,
            String createdAt) {
        jdbcTemplate.update("""
                INSERT INTO t_page_view (
                    article_id, lang, visitor_hash, created_at
                ) VALUES (?, ?, ?, ?)
                """,
                articleId,
                lang,
                hash,
                LocalDateTime.parse(createdAt));
    }
}
