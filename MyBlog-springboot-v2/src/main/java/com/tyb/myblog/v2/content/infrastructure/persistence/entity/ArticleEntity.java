package com.tyb.myblog.v2.content.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tyb.myblog.v2.common.infrastructure.persistence.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 文章持久化实体，对应 {@code t_article}。
 */
@Getter
@Setter
@TableName("t_article")
public class ArticleEntity extends BaseEntity {

    private String titleZh;

    private String titleJa;

    private String titleEn;

    private String summaryZh;

    private String summaryJa;

    private String summaryEn;

    private String body;

    private Long categoryId;

    private Long authorId;

    private String slug;

    private Integer status;

    private String homepageSlot;

    private String accessPassword;

    private LocalDateTime publishAt;

    private Long coverAttachmentId;

    private Integer commentCount;
}
