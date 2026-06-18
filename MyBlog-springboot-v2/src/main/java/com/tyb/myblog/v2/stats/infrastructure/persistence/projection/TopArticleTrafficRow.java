package com.tyb.myblog.v2.stats.infrastructure.persistence.projection;

import lombok.Getter;
import lombok.Setter;

/** TOP 文章访问 SQL 投影。 */
@Getter
@Setter
public class TopArticleTrafficRow {
    private Long articleId;
    private Long pv;
    private Long dailyUvSum;
}
