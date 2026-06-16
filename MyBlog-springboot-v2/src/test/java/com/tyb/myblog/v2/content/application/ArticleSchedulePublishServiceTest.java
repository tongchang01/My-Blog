package com.tyb.myblog.v2.content.application;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.article.ArticleSchedulePublishService;
import com.tyb.myblog.v2.content.domain.article.Article;
import com.tyb.myblog.v2.content.domain.article.ArticleRepository;
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleSchedulePublishServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-16T03:00:00Z"),
            ZoneId.of("Asia/Tokyo"));
    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 6, 16, 12, 0);

    @Mock
    private ArticleRepository repository;

    private ArticleSchedulePublishService service;

    @BeforeEach
    void setUp() {
        service = new ArticleSchedulePublishService(repository, CLOCK);
    }

    @Test
    void publishesDueScheduledArticlesAsSystem() {
        when(repository.findDueScheduledForUpdate(NOW, 50))
                .thenReturn(List.of(scheduled(10L)));
        when(repository.updateStatus(
                10L,
                ArticleStatus.SCHEDULED,
                ArticleStatus.PUBLISHED,
                NOW,
                null))
                .thenReturn(true);

        assertThat(service.publishDue()).isEqualTo(1);
        verify(repository).updateStatus(
                10L,
                ArticleStatus.SCHEDULED,
                ArticleStatus.PUBLISHED,
                NOW,
                null);
    }

    @Test
    void rollsBackWhenConditionalUpdateFails() {
        when(repository.findDueScheduledForUpdate(NOW, 50))
                .thenReturn(List.of(scheduled(10L)));
        when(repository.updateStatus(
                10L,
                ArticleStatus.SCHEDULED,
                ArticleStatus.PUBLISHED,
                NOW,
                null))
                .thenReturn(false);

        assertThatThrownBy(() -> service.publishDue())
                .isInstanceOfSatisfying(
                        ApiException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(ApiErrorCode.CONFLICT));
    }

    private Article scheduled(long id) {
        return Article.reconstitute(
                id,
                "标题",
                null,
                null,
                "摘要",
                null,
                null,
                "正文",
                10L,
                1001L,
                "article-" + id,
                ArticleStatus.SCHEDULED,
                null,
                NOW.minusMinutes(1),
                null,
                0,
                List.of(),
                NOW.minusDays(2),
                1001L,
                NOW.minusDays(1),
                1001L,
                false,
                null,
                null);
    }
}
