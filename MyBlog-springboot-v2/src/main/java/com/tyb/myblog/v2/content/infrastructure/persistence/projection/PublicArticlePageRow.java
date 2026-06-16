package com.tyb.myblog.v2.content.infrastructure.persistence.projection;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公开分页 SQL 投影，不包含正文和密码哈希。
 */
@Data
public class PublicArticlePageRow {

    private Long id;
    private String titleZh;
    private String titleJa;
    private String titleEn;
    private String summaryZh;
    private String summaryJa;
    private String summaryEn;
    private Long categoryId;
    private String categoryNameZh;
    private String categoryNameJa;
    private String categoryNameEn;
    private String slug;
    private Integer status;
    private LocalDateTime publishAt;
    private Long coverAttachmentId;
    private Integer commentCount;
    private LocalDateTime createdAt;
}
