package com.tyb.myblog.v2.content.infrastructure.persistence.projection;

import lombok.Getter;
import lombok.Setter;

/** stats 总览批量标题查询投影。 */
@Getter
@Setter
public class ArticleStatisticsSummaryRow {
    private Long id;
    private String titleZh;
}
