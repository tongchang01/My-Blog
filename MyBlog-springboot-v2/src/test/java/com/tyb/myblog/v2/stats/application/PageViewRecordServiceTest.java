package com.tyb.myblog.v2.stats.application;

import com.tyb.myblog.v2.content.application.article.PublicArticleStatisticsPolicyService;
import com.tyb.myblog.v2.stats.domain.PageViewEvent;
import com.tyb.myblog.v2.stats.domain.PageViewRepository;
import com.tyb.myblog.v2.stats.domain.VisitorHashGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PageViewRecordServiceTest {

    private final PageViewRepository repository =
            mock(PageViewRepository.class);
    private final PublicArticleStatisticsPolicyService articlePolicy =
            mock(PublicArticleStatisticsPolicyService.class);
    private final VisitorHashGenerator hashGenerator =
            mock(VisitorHashGenerator.class);
    private final PageViewRateLimitService rateLimitService =
            mock(PageViewRateLimitService.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-06-18T03:00:00Z"),
            ZoneId.of("Asia/Tokyo"));

    private PageViewRecordService service;

    @BeforeEach
    void setUp() {
        service = new PageViewRecordService(
                repository,
                articlePolicy,
                hashGenerator,
                rateLimitService,
                clock);
        when(repository.append(org.mockito.ArgumentMatchers.any()))
                .thenReturn(1L);
        when(hashGenerator.hash(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn("a".repeat(64));
    }

    @Test
    void recordsArticleViewWithJstDateAndNormalizedHeaders() {
        String longReferrer = " https://example.com/" + "r".repeat(600);

        service.record(new PageViewRecordCommand(
                100L,
                "zh",
                "203.0.113.1",
                null,
                longReferrer));

        verify(articlePolicy).requirePublicTrackable(100L);
        verify(rateLimitService).checkAndRecord("203.0.113.1");
        verify(hashGenerator).hash(
                java.time.LocalDate.of(2026, 6, 18),
                "203.0.113.1",
                "");
        ArgumentCaptor<PageViewEvent> captor =
                ArgumentCaptor.forClass(PageViewEvent.class);
        verify(repository).append(captor.capture());
        assertThat(captor.getValue().createdAt())
                .isEqualTo("2026-06-18T12:00:00");
        assertThat(captor.getValue().referrer()).hasSize(512);
    }

    @Test
    void recordsGeneralPageWithoutArticleLookup() {
        service.record(new PageViewRecordCommand(
                null,
                "en",
                "203.0.113.1",
                "JUnit",
                null));

        verify(articlePolicy, never())
                .requirePublicTrackable(org.mockito.ArgumentMatchers.anyLong());
        verify(repository).append(org.mockito.ArgumentMatchers.any());
    }
}
