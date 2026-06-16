package com.tyb.myblog.v2.content.infrastructure.persistence.projection;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台文章详情 SQL 投影，不包含密码哈希。
 */
@Data
public class AdminArticleDetailRow {

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
    private Long authorId;
    private String slug;
    private Integer status;
    private LocalDateTime publishAt;
    private Long coverAttachmentId;
    private Integer commentCount;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
}
