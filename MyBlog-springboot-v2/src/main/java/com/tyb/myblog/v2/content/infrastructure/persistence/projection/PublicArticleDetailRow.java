package com.tyb.myblog.v2.content.infrastructure.persistence.projection;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公开详情 SQL 投影，PASSWORD 只用于上层返回 403。
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
}
