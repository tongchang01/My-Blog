package com.tyb.myblog.v2.content.infrastructure.persistence.projection;

import lombok.Data;

/**
 * 文章公开查询中按文章批量读取的标签投影。
 */
@Data
public class ArticleTagRow {

    private Long articleId;
    private Long id;
    private String nameZh;
    private String nameJa;
    private String nameEn;
    private String slug;
}
