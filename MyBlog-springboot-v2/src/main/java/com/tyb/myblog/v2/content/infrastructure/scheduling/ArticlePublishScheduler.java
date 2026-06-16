package com.tyb.myblog.v2.content.infrastructure.scheduling;

import com.tyb.myblog.v2.content.application.article.ArticleSchedulePublishService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArticlePublishScheduler {

    private final ArticleSchedulePublishService service;

    @Scheduled(
            cron = "${myblog.content.article-publish.cron:0 * * * * *}",
            zone = "Asia/Tokyo")
    public void publishDueArticles() {
        service.publishDue();
    }
}
