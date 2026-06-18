package com.tyb.myblog.v2.stats.infrastructure.persistence;

import com.tyb.myblog.v2.stats.domain.StatsDashboardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class DatabaseStatsDashboardRepositoryTest {

    @Autowired
    private StatsDashboardRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetState() {
        jdbcTemplate.update("DELETE FROM t_page_view_daily");
    }

    @Test
    void readsClosedRangeWithStableTopOrdering() {
        insert(100L, "zh", "2026-06-17", 10, 7);
        insert(101L, "ja", "2026-06-17", 10, 6);
        insert(100L, "en", "2026-06-18", 5, 4);
        insert(0L, "zh", "2026-06-18", 20, 10);
        insert(999L, "zh", "2026-06-19", 100, 80);
        LocalDate from = LocalDate.of(2026, 6, 17);
        LocalDate to = LocalDate.of(2026, 6, 18);

        assertThat(repository.findTrend(from, to))
                .extracting(point -> point.pv())
                .containsExactly(20L, 25L);
        assertThat(repository.findTopArticles(from, to, 10))
                .extracting(metric -> metric.articleId())
                .containsExactly(100L, 101L);
        assertThat(repository.findTopArticles(from, to, 10).get(0)
                .dailyUvSum()).isEqualTo(11);
        assertThat(repository.findLanguages(from, to))
                .extracting(metric -> metric.language().code())
                .containsExactly("en", "ja", "zh");
    }

    private void insert(
            long articleId,
            String lang,
            String date,
            int pv,
            int uv) {
        jdbcTemplate.update("""
                INSERT INTO t_page_view_daily (
                    article_id, lang, stat_date, pv, uv
                ) VALUES (?, ?, ?, ?, ?)
                """,
                articleId,
                lang,
                LocalDate.parse(date),
                pv,
                uv);
    }
}
