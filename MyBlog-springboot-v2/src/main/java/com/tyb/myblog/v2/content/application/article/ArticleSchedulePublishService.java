package com.tyb.myblog.v2.content.application.article;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.domain.article.Article;
import com.tyb.myblog.v2.content.domain.article.ArticleRepository;
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleSchedulePublishService {

    private static final int BATCH_SIZE = 50;

    private final ArticleRepository repository;
    private final Clock clock;

    @Transactional
    public int publishDue() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Article> due = repository.findDueScheduledForUpdate(
                now, BATCH_SIZE);
        for (Article article : due) {
            if (!repository.updateStatus(
                    article.id(),
                    ArticleStatus.SCHEDULED,
                    ArticleStatus.PUBLISHED,
                    now,
                    null)) {
                throw new ApiException(ApiErrorCode.CONFLICT);
            }
        }
        return due.size();
    }
}
