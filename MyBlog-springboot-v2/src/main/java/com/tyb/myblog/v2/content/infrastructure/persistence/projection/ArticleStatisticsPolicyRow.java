package com.tyb.myblog.v2.content.infrastructure.persistence.projection;

import lombok.Getter;
import lombok.Setter;

/**
 * 文章统计策略数据库投影。
 */
@Getter
@Setter
public class ArticleStatisticsPolicyRow {

    private Long id;

    private Integer status;
}
