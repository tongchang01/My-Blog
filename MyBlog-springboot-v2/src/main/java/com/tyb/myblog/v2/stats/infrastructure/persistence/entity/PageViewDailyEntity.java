package com.tyb.myblog.v2.stats.infrastructure.persistence.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 页面访问日聚合写入实体。
 *
 * <p>该表使用复合主键且不带软删，不继承通用审计基类。</p>
 */
@Getter
@Setter
public class PageViewDailyEntity {

    private Long articleId;

    private String lang;

    private LocalDate statDate;

    private Integer pv;

    private Integer uv;
}
