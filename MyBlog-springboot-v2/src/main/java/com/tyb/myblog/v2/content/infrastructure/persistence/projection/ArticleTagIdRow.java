package com.tyb.myblog.v2.content.infrastructure.persistence.projection;

import lombok.Data;

/**
 * 按文章批量读取标签 ID 的 SQL 投影。
 */
@Data
public class ArticleTagIdRow {

    private Long articleId;
    private Long tagId;
}
