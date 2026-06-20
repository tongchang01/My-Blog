package com.tyb.myblog.v2.content.infrastructure.persistence.projection;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 已发布文章公开详情 SQL 投影。
 */
@Data
public class PublicArticleDetailRow {

    private Long id;
    private String titleZh;
    private String titleJa;
    private String titleEn;
    private String summaryZh;
    private String summaryJa;
    private String summaryEn;
    private String body;
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
    private LocalDateTime updatedAt;
}
