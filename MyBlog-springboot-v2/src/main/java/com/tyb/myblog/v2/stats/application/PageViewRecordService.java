package com.tyb.myblog.v2.stats.application;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.article.PublicArticleStatisticsPolicyService;
import com.tyb.myblog.v2.stats.domain.PageViewEvent;
import com.tyb.myblog.v2.stats.domain.PageViewRepository;
import com.tyb.myblog.v2.stats.domain.StatsLanguage;
import com.tyb.myblog.v2.stats.domain.VisitorHashGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 公开页面访问明细写入用例。
 */
@Service
@RequiredArgsConstructor
public class PageViewRecordService {

    private static final int MAX_REFERRER_LENGTH = 512;

    private final PageViewRepository repository;
    private final PublicArticleStatisticsPolicyService articlePolicy;
    private final VisitorHashGenerator hashGenerator;
    private final PageViewRateLimitService rateLimitService;
    private final Clock clock;

    public long record(PageViewRecordCommand command) {
        rateLimitService.checkAndRecord(command.clientIp());
        StatsLanguage language = parseLanguage(command.lang());
        if (command.articleId() != null) {
            articlePolicy.requirePublicTrackable(command.articleId());
        }
        LocalDateTime now = LocalDateTime.now(clock);
        String userAgent = normalize(command.userAgent());
        String visitorHash = hashGenerator.hash(
                now.toLocalDate(),
                command.clientIp(),
                userAgent);
        return repository.append(PageViewEvent.create(
                command.articleId(),
                language,
                visitorHash,
                normalizeReferrer(command.referrer()),
                now));
    }

    private StatsLanguage parseLanguage(String value) {
        try {
            return StatsLanguage.fromCode(value);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    exception.getMessage());
        }
    }

    private String normalizeReferrer(String value) {
        String normalized = normalize(value);
        if (normalized.length() <= MAX_REFERRER_LENGTH) {
            return normalized.isEmpty() ? null : normalized;
        }
        return normalized.substring(0, MAX_REFERRER_LENGTH);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
