package com.tyb.myblog.v2.stats.infrastructure.persistence;

import com.tyb.myblog.v2.stats.domain.PageViewEvent;
import com.tyb.myblog.v2.stats.domain.PageViewRepository;
import com.tyb.myblog.v2.stats.domain.StatsLanguage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class DatabasePageViewRepositoryTest {

    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 6, 18, 12, 0);

    @Autowired
    private PageViewRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetState() {
        jdbcTemplate.update("DELETE FROM t_page_view_daily");
        jdbcTemplate.update("DELETE FROM t_page_view");
    }

    @Test
    void appendsArticleAndGeneralViewsWithAutoIncrementIds() {
        long first = repository.append(event(
                100L,
                StatsLanguage.ZH,
                "a".repeat(64)));
        long second = repository.append(event(
                null,
                StatsLanguage.EN,
                "b".repeat(64)));

        assertThat(first).isPositive();
        assertThat(second).isGreaterThan(first);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_page_view",
                Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT article_id FROM t_page_view WHERE id = ?",
                Long.class,
                first)).isEqualTo(100L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT lang FROM t_page_view WHERE id = ?",
                String.class,
                second)).isEqualTo("en");
    }

    private PageViewEvent event(
            Long articleId,
            StatsLanguage language,
            String visitorHash) {
        return PageViewEvent.create(
                articleId,
                language,
                visitorHash,
                "https://referrer.example",
                NOW);
    }
}
