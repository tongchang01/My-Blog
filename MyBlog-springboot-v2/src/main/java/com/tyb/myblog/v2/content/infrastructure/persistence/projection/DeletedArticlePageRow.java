package com.tyb.myblog.v2.content.infrastructure.persistence.projection;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeletedArticlePageRow {

    private Long id;
    private String titleZh;
    private String titleJa;
    private String titleEn;
    private Integer status;
    private Long categoryId;
    private LocalDateTime deletedAt;
    private Long deletedBy;
}
