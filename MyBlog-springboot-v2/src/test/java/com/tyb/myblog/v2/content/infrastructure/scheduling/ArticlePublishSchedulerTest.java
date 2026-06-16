package com.tyb.myblog.v2.content.infrastructure.scheduling;

import com.tyb.myblog.v2.content.application.article.ArticleSchedulePublishService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ArticlePublishSchedulerTest {

    @Test
    void delegatesToPublishService() {
        ArticleSchedulePublishService service =
                mock(ArticleSchedulePublishService.class);
        ArticlePublishScheduler scheduler =
                new ArticlePublishScheduler(service);

        scheduler.publishDueArticles();

        verify(service).publishDue();
    }
}
