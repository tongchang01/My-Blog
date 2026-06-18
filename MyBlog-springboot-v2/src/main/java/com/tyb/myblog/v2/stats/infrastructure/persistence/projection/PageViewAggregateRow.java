package com.tyb.myblog.v2.stats.infrastructure.persistence.projection;

import lombok.Getter;
import lombok.Setter;

/**
 * 访问明细聚合 SQL 投影。
 */
@Getter
@Setter
public class PageViewAggregateRow {

    private Long articleId;

    private String lang;

    private Integer pv;

    private Integer uv;
}
