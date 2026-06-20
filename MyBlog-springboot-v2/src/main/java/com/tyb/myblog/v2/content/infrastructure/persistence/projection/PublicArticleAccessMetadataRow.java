package com.tyb.myblog.v2.content.infrastructure.persistence.projection;

import lombok.Data;

/**
 * 公开文章访问预检 SQL 投影，不读取正文。
 */
@Data
public class PublicArticleAccessMetadataRow {

    private Long id;
    private Integer status;
}
