package com.tyb.myblog.v2.content.infrastructure.scheduling;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(
        prefix = "myblog.content.article-publish",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class ArticleSchedulingConfiguration {
}
